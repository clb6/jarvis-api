(ns jarvis-api.handler
  (:require [compojure.api.sweet :refer :all]
            [ring.util.http-response :refer :all]
            [schema.core :as s]
            [clojure.string :as cs]
            [clj-time.core :as t]
            [clj-time.format :as tf]))

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

(def jarvis-root-directory (System/getenv "JARVIS_DIR_ROOT"))
(def jarvis-log-directory (cs/join "/" [jarvis-root-directory "LogEntries"]))
(def jarvis-images-directory (cs/join "/" [jarvis-root-directory "Images"]))
(def jarvis-tag-directory (cs/join "/" [jarvis-root-directory "Tags"]))

(def jarvis-tag-version "0.1.0")

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

(defn markdown-to-html-images
  "Takes a body of markdown text and replaces the image syntax with html image
  tags. Returns the new version of the text."
  [body]
  (cs/replace body #"!\[([\-\w]*)\]\(([\w.\-\/]*)\)"
              (format "<img src=\"file://%s/$2\" alt=\"$1\" height=\"750px\" width=\"750px\" />"
                      jarvis-images-directory)))

(defn parse-file
  [text]
  (let [[metadata & body] (cs/split text #"\n\n")]
    (assoc (parse-file-metadata metadata) :body
           (markdown-to-html-images (cs/join "\n\n" body)))))

(defn get-log-entry!
  [id]
  (let [log-entry-path (format "%s/%s.md" jarvis-log-directory id)
        log-entry (slurp log-entry-path)]
    (parse-file log-entry)))


(defn fetch-tag-files!
  "Returns a list of all available tags as Java Files"
  []
  (filter #(.isFile %1) (file-seq (clojure.java.io/file jarvis-tag-directory))))

(defn filter-tag-files-by-tag-name
  "Function extracts the Java File with a matching name which means matches by a
  case-insensitive version of the file name. Returns only the first match."
  [tag-name tag-files]
  (letfn [(tag-file-to-name [tag-file]
            ((comp clojure.string/lower-case
                   #(clojure.string/replace %1 #".md" ""))
             (.getName tag-file)))]
  (first (filter #(= tag-name (tag-file-to-name %1)) tag-files))))

(defn get-tag-object!
  "Returns the map representation of a given tag name"
  [tag-name]
  (let [tag-file (filter-tag-files-by-tag-name tag-name (fetch-tag-files!))]
    (if tag-file
      (let [tag-content (slurp tag-file)]
        (parse-file tag-content)))))


(defn generate-tag-file-metadata
  [tag-object]
  (let [metadata-keys (list :author :created :version :tags)]
    (letfn [(create-line [mk]
              (cs/join ": " (list (cs/capitalize (name mk)) (get tag-object mk))))]
      (cs/join "\n" (map create-line metadata-keys)))))

(defn generate-tag-file
  [tag-object]
  (cs/join "\n\n" (list (generate-tag-file-metadata tag-object)
                        (get tag-object :body))))

(defn create-tag-object
  "Create Tag from TagRequest"
  [tag-request]
  (let [now-isoformat (tf/unparse (tf/formatters :date-hour-minute-second) (t/now))]
    (dissoc (assoc tag-request :created now-isoformat :version jarvis-tag-version)
            :name)))

(defn post-tag-object!
  "Takes a TagRequest converts to a Tag which is written to the filesystem in the
  tag file format"
  [tag-request]
  (let [tag-name (get tag-request :name)
        tag-file-path (format "%s/%s.md" jarvis-tag-directory tag-name)]
    (spit tag-file-path (generate-tag-file (create-tag-object tag-request)))))


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
      (ok (get-log-entry! id))))
  (context* "/tags" []
    :tags ["tags"]
    :summary "API to handle tags"
    (GET* "/:tag-name" [tag-name]
      :responses {200 {:schema Tag :description "Return found tag"}
                  404 {:schema WebError :description "Tag not found"}}
      (if-let [tag-object (get-tag-object! tag-name)]
        (ok tag-object)
        (not-found { :message "Unknown tag" })))
    ;(POST* "/" []
    ;  :return { :created_tags [s/Str] }
    ;  :body [tag-object-new Tag]
            )
  )
