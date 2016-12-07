(ns jarvis-api.handler
  (:require [compojure.api.sweet :refer :all]
            [clojure.string :as cs]
            [ring.util.http-response :refer :all]
            [schema.core :as s]
            [ring.middleware.logger :as log]
            [org.bovinegenius.exploding-fish :as ef]
            [taoensso.timbre :as timbre :refer [set-level! info]]
            [cheshire.core :refer [parse-string]]
            [jarvis-api.schemas :refer [LogEntryRequest LogEntry
                                        Tag TagRequest DataSummary Link
                                        Event EventRequest event-categories]]
            [jarvis-api.links :as jl]
            [jarvis-api.resources.tags :as tags]
            [jarvis-api.resources.logentries :as logs]
            [jarvis-api.resources.events :as events]
            [jarvis-api.resources.datasummary :as dsummary]
            [jarvis-api.database.elasticsearch :as jes]
            [jarvis-api.config :as config]
            ))


(defn- find-missing-tags
  "Find the list of tags that do not exist if there are no missing tags then
  return nothing. This method is to work around the poor truthiness evaluation by
  Clojure - empty vectors are considered True and not False."
  [jarvis-request]
  (let [tag-names-missing (tags/filter-tag-names-missing (:tags jarvis-request))]
    (if (not (empty? tag-names-missing))
      tag-names-missing)))

(defn- wrap-check-tags-exist
  ([handler jarvis-request]
   ; By default, do not skip the tags check
   (wrap-check-tags-exist handler jarvis-request false))
  ([handler jarvis-request should-skip]
   (fn check-tags-exist [request]
     (if-let [tag-names-missing (find-missing-tags jarvis-request)]
       (do
         ; TODO: Change to log
         (println "Tags missing: " (pr-str tag-names-missing))
         (if should-skip
           (handler jarvis-request)
           (bad-request { :error "Unknown tags.", :missing-tags tag-names-missing })))
       (handler jarvis-request)))))

(defn- wrap-request-add-self-link
  [handler]
  (fn add-self-link [{:keys [scheme server-name server-port uri query-params] :as r}]
    (let [query-string (cs/join "&" (map #(cs/join "=" %1) (seq query-params)))
          self-uri (ef/uri { :scheme (name scheme) :host server-name :port server-port
                             :authority (cs/join ":" [server-name, server-port])
                             :path uri :query query-string})]
      (handler (assoc r :fully-qualified-uri self-uri)))))

(defn- wrap-check-event-id
  ; Middleware to check that an event id is valid before proceeding.
  [handler]
  (fn check-event-id [{:keys [params] :as r}]
      (let [event-id (:event-id params)]
        (if (events/get-event! event-id)
          (handler r)
          (not-found { :error "Unknown event" })
          ))
      )
  )


(defapi app
  ; TODO: Should revisit and enhance the use of swagger-routes. Use :ui, :spec options
  ; https://github.com/metosin/compojure-api/wiki/Migration-Guide-to-1.0.0#swagger-routes
  (swagger-routes
    { :data { :info { :title "jarvis-api" :description "jarvis data api" }
              :tags [{:name "logentries" :description ""}
                     {:name "tags" :description ""}]}})
  (context "/datasummary/:data-type" []
           :tags ["datasummary"]
           :summary "Endpoint that provides a summary of jarvis resource types"
           (GET "/" [data-type]
                :path-params [data-type :- (s/enum :tags :logentries :events)]
                :return DataSummary
                (ok (dsummary/generate-data-summary data-type))))
  (context "/search" []
           :tags ["search"]
           :middleware [wrap-request-add-self-link]
           (GET "/logentries" [:as {:keys [fully-qualified-uri]}]
                :query-params [{tags :- s/Str nil} {searchterm :- s/Str nil}
                               {from :- Long 0}]
                :return { :items [LogEntry], :total Long, :links [Link] }
                (let [query-result (logs/query-log-entries tags searchterm from)
                      response (assoc query-result :items
                                      (map (partial jl/expand-log-entry
                                                    fully-qualified-uri)
                                           (:items query-result)))
                      response (assoc response :links
                                      (jl/generate-query-links (:total query-result)
                                                               from
                                                               fully-qualified-uri))]
                  (ok response))))
  (context "/events/:event-id/logentries" []
           :tags ["logentries"]
           :middleware [wrap-request-add-self-link wrap-check-event-id]
           (GET "/:id" [:as {:keys [fully-qualified-uri]}]
                ; Don't know why BigInteger is not acceptable.
                :path-params [id :- s/Int event-id :- s/Str]
                :return LogEntry
                (if-let [log-entry (logs/get-log-entry! id)]
                  (ok (jl/expand-log-entry fully-qualified-uri event-id log-entry))
                  (not-found)))
           (POST "/" [:as {:keys [fully-qualified-uri]}]
                 :return LogEntry
                 :path-params [event-id :- s/Str]
                 :body [log-entry-request LogEntryRequest]
                 (-> (fn [log-entry-request]
                       (if-let [log-entry-object (logs/post-log-entry! log-entry-request
                                                                       event-id)]
                         (let [log-entry (jl/expand-log-entry fully-qualified-uri
                                                              event-id
                                                              log-entry-object)]
                           (header (created log-entry) "Location"
                                   (jl/construct-new-log-entry-uri fully-qualified-uri
                                                                   event-id
                                                                   (:id log-entry-object))))
                         (internal-server-error)))
                     (wrap-check-tags-exist log-entry-request)))
           (PUT "/:id" [:as {:keys [fully-qualified-uri]}]
                :path-params [id :- s/Int event-id :- s/Str]
                :return LogEntry
                :body [log-entry-request LogEntryRequest]
                (-> (fn [log-entry-request]
                      (if-let [log-entry-object (logs/get-log-entry! id)]
                        (let [log-entry-object (logs/update-log-entry! log-entry-object
                                                                       log-entry-request)
                              log-entry (jl/expand-log-entry fully-qualified-uri
                                                             event-id
                                                             log-entry-object)]
                          (header (ok log-entry) "Location" fully-qualified-uri))
                        (not-found)))
                    (wrap-check-tags-exist log-entry-request))))
  (context "/tags" []
           :tags ["tags"]
           :summary "API to handle tags"
           :middleware [wrap-request-add-self-link]
           (GET "/" [:as {:keys [fully-qualified-uri]}]
                :query-params [{name :- s/Str nil} {tags :- s/Str nil}
                                {from :- Long 0}]
                :return { :items [Tag], :total Long, :links [Link] }
                (let [query-result (tags/query-tags name tags from)
                      response (assoc query-result :items
                                      (map (partial jl/expand-tag
                                                    fully-qualified-uri)
                                           (:items query-result)))
                      response (assoc response :links
                                      (jl/generate-query-links (:total query-result)
                                                               from
                                                               fully-qualified-uri))]
                  (ok response)))
           (GET "/:tag-name" [:as {:keys [fully-qualified-uri]}]
                :path-params [tag-name :- s/Str]
                :return Tag
                (if (tags/tag-exists? tag-name)
                  (ok (jl/expand-tag fully-qualified-uri (tags/get-tag! tag-name)))
                  (not-found)))
           (POST "/" [:as {:keys [fully-qualified-uri]}]
                 ; skipTagsCheck is intended to be used specifically for
                 ; migrations. This is needed because tags can have circular
                 ; relationships.
                 :query-params [{skipTagsCheck :- s/Bool false}]
                 :return Tag
                 :body [tag-request TagRequest]
                 (-> (fn [tag-request]
                       (if (tags/tag-exists? (:name tag-request))
                         (conflict)
                         (if-let [tag-object (tags/post-tag! tag-request)]
                           (let [tag (jl/expand-tag fully-qualified-uri
                                                          tag-object)]
                             (header (created tag) "Location"
                                     (jl/construct-new-tag-uri (:name tag-object)
                                                               fully-qualified-uri)))
                           (internal-server-error))))
                     (wrap-check-tags-exist tag-request skipTagsCheck)))
           (PUT "/:tag-name" [:as {:keys [fully-qualified-uri]}]
                :path-params [tag-name :- s/Str]
                :return Tag
                :body [tag-request TagRequest]
                (-> (fn [tag-request]
                      (if (tags/valid-tag? tag-name tag-request)
                        (if-let [tag-object-prev (tags/get-tag! tag-name)]
                          (let [tag-object (tags/update-tag! tag-object-prev
                                                             tag-request)
                                tag (jl/expand-tag fully-qualified-uri
                                                   tag-object)]
                            (header (ok tag) "Location" fully-qualified-uri))
                          (not-found))
                        (bad-request)))
                    (wrap-check-tags-exist tag-request))))
  (context "/events" []
           :tags ["events"]
           :summary "API to handle events"
           :middleware [wrap-request-add-self-link]
           (GET "/" [:as {:keys [fully-qualified-uri]}]
                :query-params [{category :- (describe event-categories "Filter by specified event category") nil}
                               {weight :- (describe Long "Filter out all events that have a numerical weight less than this") 0}
                               {searchterm :- (describe s/Str "Query descriptions using this single search term") nil }
                               {from :- (describe Long "Events index used for pagination") 0}]
                :return { :items [Event], :total Long, :links [Link] }
                (let [query-result (events/query-events category weight searchterm from)
                      response (assoc query-result :links
                                      (jl/generate-query-links (:total query-result)
                                                               from
                                                               fully-qualified-uri))
                      response (assoc response :items
                                      (map (partial jl/expand-event fully-qualified-uri)
                                           (:items response)))]
                  (ok response)))
           (GET "/:event-id" [:as {:keys [fully-qualified-uri]}]
                :path-params [event-id :- s/Str]
                :return Event
                (if-let [event-mixin (events/get-event! event-id)]
                  (ok (jl/expand-event fully-qualified-uri event-mixin))
                  (not-found)))
           (POST "/" [:as {:keys [fully-qualified-uri]}]
                 :return Event
                 :body [event-request EventRequest]
                 (let [event-mixin (events/post-event! event-request)
                       event (jl/expand-event fully-qualified-uri event-mixin)]
                   (header (created event) "Location"
                           (jl/construct-new-event-uri (:eventId event)
                                                       fully-qualified-uri))))
           (PUT "/:event-id" [:as {:keys [fully-qualified-uri]}]
                :path-params [event-id :- s/Str]
                :return Event
                :body [event-request EventRequest]
                (if-let [event-mixin (events/get-event! event-id)]
                  (let [event-mixin (events/update-event! event-mixin event-request)
                        event (jl/expand-event fully-qualified-uri event-mixin)]
                    (header (ok event) "Location" fully-qualified-uri))
                  (not-found)))
           )
  )


(def app-with-logging (log/wrap-with-logger app))

(defn init-app
  []
  (set-level! :info)

  (letfn [(parse-mapping [mapping-file]
            (let [type-name (second (re-find #"mapping_(\w+)\.json"
                                             (.getName mapping-file)))
                  type-properties (parse-string (slurp mapping-file) true)]
              [type-name type-properties]))]

    (let [mapping-files (file-seq (clojure.java.io/file
                                    config/elasticsearch-mappings-directory))
          mapping-files (filter #(.isFile %) mapping-files)]

      (jes/initialize! (map parse-mapping mapping-files))
    ))

  (info "Done initializing application")
  )
