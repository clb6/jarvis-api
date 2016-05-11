(ns jarvis-api.resources.tags
  (:require [clojure.string :as cs]
            [schema.core :as s]
            [jarvis-api.schemas :refer [TagObject TagRequest]]
            [jarvis-api.config :as config]
            [jarvis-api.markdown_filer :as mf]
            [jarvis-api.data_access :as jda]
            [jarvis-api.util :as util]
            [jarvis-api.links :as jl]))


(defn query-tags
  [tag-name tags from]
  (let [query-criterias (jda/add-query-criteria-tag-name tag-name)
        query-criterias (jda/add-query-criteria-tags tags query-criterias)
        query-result (jda/query-tags query-criterias from)]
    { :items (jda/get-hits-from-query query-result)
      :total (jda/get-total-hits-from-query query-result) }))


(defn- fetch-tag-files!
  "Returns a list of all available tags as Java Files"
  []
  (filter #(.isFile %1) (file-seq (clojure.java.io/file config/jarvis-tag-directory))))

(defn- filter-tag-files-by-tag-name
  "Function extracts the Java File with a matching name which means matches by a
  case-insensitive version of the file name. Returns only the first match."
  [tag-name tag-files]
  (let [tag-name (cs/lower-case tag-name)]
    (letfn [(tag-file-to-name [tag-file]
              ((comp cs/lower-case
                     #(clojure.string/replace %1 #".md" ""))
               (.getName tag-file)))]
    (first (filter #(= tag-name (tag-file-to-name %1)) tag-files)))))


(s/defn get-tag! :- TagObject
  "Returns web response where it will return a tag object if a tag is found"
  [tag-name :- String]
  (jda/get-jarvis-document! "tags" (cs/lower-case tag-name)))

(defn tag-exists?
  "Case-insensitive check of whether a tag already exists"
  [tag-name]
  ((comp not nil?) (get-tag! tag-name)))

(s/defn filter-tag-names-missing :- [s/Str]
  "Given a list of tag names, returns the list of tag names that are missing"
  [tag-names :- [s/Str]]
  (filter #(not (tag-exists? %1)) tag-names))


(def metadata-keys-tags (list :name :author :created :modified :version :tags))
(def create-tag-file (partial mf/create-file metadata-keys-tags))

(defn- write-tag-object!
  [tag-name tag-object]
  (let [tag-file-path (format "%s/%s.md" config/jarvis-tag-directory tag-name)]
    (jda/write-jarvis-document! "tags" tag-file-path create-tag-file
                                (cs/lower-case tag-name) tag-object)))

(defn- create-tag-object
  "Create Tag from TagRequest"
  [tag-request]
  (let [modified-isoformat (util/create-timestamp-isoformat)
        ; Allow created timestamp to be passed-in. The use case is migrations.
        created-isoformat (if (contains? tag-request :created)
                            (:created tag-request)
                            modified-isoformat)]
    (assoc tag-request :created created-isoformat :modified modified-isoformat
           :version config/jarvis-tag-version)))


(s/defn valid-tag?
  [tag-name :- s/Str tag-request :- TagRequest]
  (= (cs/lower-case tag-name) (cs/lower-case (:name tag-request))))


(s/defn post-tag! :- TagObject
  "Takes a TagRequest converts to a Tag which is written to the filesystem in the
  tag file format."
  [tag-request :- TagRequest]
  (write-tag-object! (:name tag-request) (create-tag-object tag-request)))


(s/defn update-tag! :- TagObject
  [tag-object :- TagObject tag-request :- TagRequest]
  (let [updated-tag-object (merge tag-object tag-request)
        updated-tag-object (assoc updated-tag-object :modified (util/create-timestamp-isoformat))]
    (write-tag-object! (:name updated-tag-object) updated-tag-object)))
