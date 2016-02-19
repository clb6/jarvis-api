(ns jarvis-api.handler
  (:require [compojure.api.sweet :refer :all]
            [ring.util.http-response :refer :all]
            [schema.core :as s]
            [jarvis-api.resources.tags :as tags]
            [jarvis-api.resources.logentries :as logs]))


(s/defschema LogEntry { :author String
                       :created String
                       :occurred String
                       :version String
                       :tags [s/Str]
                       :parent (s/maybe s/Str)
                       :todo (s/maybe s/Str)
                       :setting String
                       :body String })

(s/defschema Tag { :author s/Str
                  :created s/Str
                  :version s/Str
                  :tags [s/Str]
                  :body s/Str })

(s/defschema TagRequest { :name s/Str
                         :author s/Str
                         :tags [s/Str]
                         :body s/Str })

(s/defschema WebError { :message s/Str })


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
      (ok (logs/get-log-entry! id))))
  (context* "/tags" []
    :tags ["tags"]
    :summary "API to handle tags"
    (GET* "/:tag-name" [tag-name]
      :responses {200 {:schema Tag :description "Return found tag"}
                  404 {:schema WebError :description "Tag not found"}}
      (tags/get-tag! tag-name))
    (POST* "/" []
      :return { :tags_missing [s/Str] }
      :body [tag-request TagRequest]
      (tags/post-tag! tag-request)))
  )
