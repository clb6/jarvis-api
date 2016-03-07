(ns jarvis-api.handler
  (:require [compojure.api.sweet :refer :all]
            [ring.util.http-response :refer :all]
            [schema.core :as s]
            [ring.middleware.logger :as log]
            [clj-logging-config.log4j :refer [set-loggers!]]
            [jarvis-api.schemas :refer [LogEntry LogEntryRequest LogEntryPrev
                                        Tag TagRequest TagPrev]]
            [jarvis-api.resources.tags :as tags]
            [jarvis-api.resources.logentries :as logs]))


(defn query-log-entries
  [tag search-term]
  [{:body (str "query: " tag ", " search-term)}])

(defn- find-missing-tags
  "Find the list of tags that do not exist if there are no missing tags then
  return nothing. This method is to work around the poor truthiness evaluation by
  Clojure - empty vectors are considered True and not False."
  [jarvis-request]
  (let [tag-names-missing (tags/filter-tag-names-missing (:tags jarvis-request))]
    (if (not (empty? tag-names-missing))
      tag-names-missing)))



(defapi app
  (swagger-ui)
  (swagger-docs
    {:info {:title "Jarvis-api"
            :description "Jarvis data api"}
     :tags [{:name "logentries" :description "handles Jarvis log entries"}
            {:name "tags" :description "handles Jarvis tags"}]})
  (context* "/logentries" []
    :tags ["logentries"]
    :summary "API to handle log entries"
    (GET* "/" []
      :return [LogEntry]
      :query-params [{tag :- String ""}
                     {searchterm :- String ""}]
      (ok (query-log-entries tag searchterm)))
    (GET* "/:id" [id]
      ; Don't know why BigInteger is not acceptable.
      :path-params [id :- Long]
      :return LogEntry
      (if-let [log-entry (logs/get-log-entry! id)]
        (ok log-entry)
        (not-found)))
    (POST* "/" []
      :return LogEntry
      :body [log-entry-request LogEntryRequest]
      (if-let [tag-names-missing (find-missing-tags log-entry-request)]
        (bad-request { :error "There are unknown tags.", :missing-tags tag-names-missing })
        (ok (logs/post-log-entry! log-entry-request))))
    (PUT* "/:id/migrate" [id]
      :path-params [id :- Long]
      :return LogEntry
      :body [log-entry-to-migrate LogEntryPrev]
      ; TODO: Validate the previous log entry object
      (if (logs/log-entry-exists? id)
        (conflict)
        (ok (logs/migrate-log-entry! id log-entry-to-migrate)))))
  (context* "/tags" []
    :tags ["tags"]
    :summary "API to handle tags"
    (GET* "/:tag-name" [tag-name]
      :return Tag
      (if (tags/tag-exists? tag-name)
        (ok (tags/get-tag! tag-name))
        (not-found)))
    (POST* "/" []
      :return Tag
      :body [tag-request TagRequest]
      (if (tags/tag-exists? (:name tag-request))
        (conflict)
        (if-let [tag-names-missing (find-missing-tags tag-request)]
          (bad-request { :error "There are unknown tags.", :missing-tags tag-names-missing })
          (ok (tags/post-tag! tag-request)))))
    (PUT* "/:tag-name/migrate" [tag-name]
        :return Tag
        :body [tag-to-migrate TagPrev]
        ; TODO: Validate the previous tag object
        (if (tags/tag-exists? tag-name)
          (conflict)
          (ok (tags/migrate-tag! tag-name tag-to-migrate)))))
  )


(def app-with-logging (log/wrap-with-logger app))

(defn init-app
  []
  (set-loggers! :root { :level :info }))
