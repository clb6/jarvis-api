(ns jarvis-api.handler
  (:require [compojure.api.sweet :refer :all]
            [ring.util.http-response :refer :all]
            [schema.core :as s]
            [clojure.string :as cs]))

(s/defschema LogEntry { :author String
                       :created String
                       :occurred String
                       :version String
                       :tags [s/Str]
                       :parent (s/maybe s/Str)
                       :todo (s/maybe s/Str)
                       :setting String
                       :body String })

(def jarvis-root-directory (System/getenv "JARVIS_DIR_ROOT"))
(def jarvis-log-directory (cs/join "/" [jarvis-root-directory "LogEntries"]))

(defn query-log-entries
  [tag search-term]
  [{:body (str "query: " tag ", " search-term)}])

(defn parse-file-metadata
  "Takes a metadata string, splits each line, then each line is split per
  key-value pair.

  e.g. 'Author: John Doe' becomes { :author 'John Doe' }"
  [metadata]
  (let [[:as metadata-tuples] (map #(cs/split %1 #": ") (cs/split metadata #"\n"))]
    (update-in (reduce (fn [target-map [k v]]
                         (assoc target-map ((comp keyword cs/lower-case) k) v))
                       {}
                       metadata-tuples)
               [:tags] #(cs/split %1 #", "))))

(defn parse-file
  [text]
  (let [[metadata & body] (cs/split text #"\n\n")]
    (assoc (parse-file-metadata metadata) :body (cs/join "\n\n" body))))

(defn get-log-entry!
  [id]
  (let [log-entry-path (format "%s/%s.md" jarvis-log-directory id)
        log-entry (slurp log-entry-path)]
    (parse-file log-entry)))

(defapi app
  (swagger-ui)
  (swagger-docs
    {:info {:title "Jarvis-api"
            :description "Jarvis data api"}
     :tags [{:name "logentries", :description "handles Jarvis log entries"}]})
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
      (ok (get-log-entry! id)))))
