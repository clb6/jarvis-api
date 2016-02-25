(ns jarvis-api.handler
  (:require [compojure.api.sweet :refer :all]
            [ring.util.http-response :refer :all]
            [schema.core :as s]
            [jarvis-api.schemas :refer [LogEntry LogEntryRequest Tag TagRequest]]
            [jarvis-api.resources.tags :as tags]
            [jarvis-api.resources.logentries :as logs]))


(defn query-log-entries
  [tag search-term]
  [{:body (str "query: " tag ", " search-term)}])


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
      :return LogEntry
      (logs/get-log-entry! id))
    (POST* "/" []
      :body [log-entry-request LogEntryRequest]
      (logs/post-log-entry! log-entry-request)))
  (context* "/tags" []
    :tags ["tags"]
    :summary "API to handle tags"
    (GET* "/:tag-name" [tag-name]
      :return Tag
      (tags/get-tag! tag-name))
    (POST* "/" []
      :return { :tags_missing [s/Str] }
      :body [tag-request TagRequest]
      (tags/post-tag! tag-request)))
  )
