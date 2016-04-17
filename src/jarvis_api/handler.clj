(ns jarvis-api.handler
  (:require [compojure.api.sweet :refer :all]
            [clojure.string :as cs]
            [ring.util.http-response :refer :all]
            [schema.core :as s]
            [ring.middleware.logger :as log]
            [clj-logging-config.log4j :refer [set-loggers!]]
            [org.bovinegenius.exploding-fish :as ef]
            [jarvis-api.schemas :refer [LogEntry LogEntryRequest LogEntryPrev
                                        Tag TagRequest TagPrev DataSummary Link]]
            [jarvis-api.links :as jl]
            [jarvis-api.resources.tags :as tags]
            [jarvis-api.resources.logentries :as logs]
            [jarvis-api.resources.datasummary :as dsummary]))


(defn- find-missing-tags
  "Find the list of tags that do not exist if there are no missing tags then
  return nothing. This method is to work around the poor truthiness evaluation by
  Clojure - empty vectors are considered True and not False."
  [jarvis-request]
  (let [tag-names-missing (tags/filter-tag-names-missing (:tags jarvis-request))]
    (if (not (empty? tag-names-missing))
      tag-names-missing)))

(defn- wrap-verify-tags
  [handler jarvis-request]
  (if-let [tag-names-missing (find-missing-tags jarvis-request)]
    (bad-request { :error "There are unknown tags.", :missing-tags tag-names-missing })
    (handler jarvis-request)))

(defn- create-web-response
  [jarvis-object]
  (if jarvis-object
    (ok jarvis-object)
    (internal-server-error)))

(defn- wrap-request-add-self-link
  [handler]
  (fn add-self-link [{:keys [scheme server-name server-port uri query-params] :as r}]
    (let [query-string (cs/join "&" (map #(cs/join "=" %1) (seq query-params)))
          self-uri (ef/uri { :scheme (name scheme) :host server-name :port server-port
                             :authority (cs/join ":" [server-name, server-port])
                             :path uri :query query-string})]
      (handler (assoc r :fully-qualified-uri self-uri)))))


(defapi app
  ; TODO: Should revisit and enhance the use of swagger-routes. Use :ui, :spec options
  ; https://github.com/metosin/compojure-api/wiki/Migration-Guide-to-1.0.0#swagger-routes
  (swagger-routes
    { :data { :info { :title "Jarvis-api" :description "Jarvis data api" }
              :tags [{:name "logentries" :description "handles Jarvis log entries"}
                     {:name "tags" :description "handles Jarvis tags"}]}})
  (context "/datasummary/:data-type" []
            :summary "Endpoint that provides a summary of Jarvis data type"
            (GET "/" [data-type]
                  :path-params [data-type :- (s/enum :tags :logentries)]
                  :return DataSummary
                  (ok (dsummary/generate-data-summary data-type))))
  (context "/logentries" []
           :tags ["logentries"]
           :summary "API to handle log entries"
           :middleware [wrap-request-add-self-link]
           (GET "/" [:as {:keys [fully-qualified-uri]}]
                :query-params [{tags :- s/Str ""} {searchterm :- s/Str ""}
                               {from :- Long 0}]
                :return { :items [LogEntry], :total Long, :links [Link] }
                (let [query-result (logs/query-log-entries tags searchterm from)
                      response (assoc query-result :links
                                      (jl/generate-query-links (:total query-result)
                                                               from
                                                               fully-qualified-uri))]
                  (ok response)))
           (GET "/:id" [:as {:keys [fully-qualified-uri id]}]
                ; Don't know why BigInteger is not acceptable.
                :path-params [id :- Long]
                :return (dissoc (merge LogEntry { :tagLinks [Link]
                                                  :parentLink (s/maybe Link) })
                                :tags :parent)
                (if-let [log-entry (logs/get-log-entry! id)]
                  (ok (jl/expand-log-entry fully-qualified-uri log-entry))
                  (not-found)))
    (POST "/" []
           :return LogEntry
           :body [log-entry-request LogEntryRequest]
           (wrap-verify-tags (fn [log-entry-request]
                               (create-web-response (logs/post-log-entry!
                                                      log-entry-request)))
                             log-entry-request))
    (PUT "/:id" [id]
          :path-params [id :- Long]
          :return LogEntry
          :body [log-entry-updated LogEntry]
          (wrap-verify-tags (fn [log-entry-updated]
                              (if (logs/valid-log-entry? id log-entry-updated)
                                (create-web-response (logs/put-log-entry! id
                                                                          log-entry-updated))
                                (bad-request)))
                            log-entry-updated))
    (PUT "/:id/migrate" [id]
      :path-params [id :- Long]
      :return LogEntry
      :body [log-entry-to-migrate LogEntryPrev]
      ; TODO: Validate the previous log entry object
      (if (logs/log-entry-exists? id)
        (conflict)
        (create-web-response (logs/migrate-log-entry! id log-entry-to-migrate)))))
  (context "/tags" []
           :tags ["tags"]
           :summary "API to handle tags"
           :middleware [wrap-request-add-self-link]
           (GET "/" [:as {:keys [fully-qualified-uri]}]
                 :query-params [{name :- s/Str ""} {tags :- s/Str ""}
                                {from :- Long 0}]
                 :return { :items [Tag], :total Long, :links [Link] }
                 (ok (tags/query-tags name tags from fully-qualified-uri)))
    (GET "/:tag-name" [tag-name]
          :return Tag
          (if (tags/tag-exists? tag-name)
            (create-web-response (tags/get-tag! tag-name))
            (not-found)))
    (POST "/" []
           :return Tag
           :body [tag-request TagRequest]
           (wrap-verify-tags (fn [tag-request]
                               (if (tags/tag-exists? (:name tag-request))
                                 (conflict)
                                 (create-web-response (tags/post-tag! tag-request))))
                             tag-request))
    (PUT "/:tag-name" [tag-name]
          :return Tag
          :body [tag-updated Tag]
          (wrap-verify-tags (fn [tag-updated]
                              (if (tags/tag-exists? tag-name)
                                (if (tags/valid-tag? tag-name tag-updated)
                                  (create-web-response (tags/put-tag! tag-name
                                                                      tag-updated))
                                  (bad-request))
                                (not-found)))
                            tag-updated))
    (PUT "/:tag-name/migrate" [tag-name]
          :return Tag
          :body [tag-to-migrate TagPrev]
          ; TODO: Validate the previous tag object
          (if (tags/tag-exists? tag-name)
            (conflict)
            (create-web-response (tags/migrate-tag! tag-name tag-to-migrate)))))
  )


(def app-with-logging (log/wrap-with-logger app))

(defn init-app
  []
  (set-loggers! :root { :level :info }))
