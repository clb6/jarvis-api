(ns jarvis-api.resources.tags
  (:require [clojure.string :as cs]
            [clj-time.core :as tc]
            [clj-time.format :as tf]
            [schema.core :as s]
            [jarvis-api.schemas :refer [Tag TagRequest TagPrev]]
            [jarvis-api.config :as config]
            [jarvis-api.markdown_filer :as mf]
            [jarvis-api.data_access :as jda]))


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

(defn tag-exists?
  "Case-insensitive check of whether a tag already exists"
  [tag-name]
  ((comp not nil?) (filter-tag-files-by-tag-name tag-name (fetch-tag-files!))))

(s/defn filter-tag-names-missing :- [s/Str]
  "Given a list of tag names, returns the list of tag names that are missing"
  [tag-names :- [s/Str]]
  (filter #(not (tag-exists? %1)) tag-names))

(s/defn get-tag! :- Tag
  "Returns web response where it will return a tag object if a tag is found"
  [tag-name :- String]
  (let [tag-file (filter-tag-files-by-tag-name tag-name (fetch-tag-files!))]
    (if tag-file
      (let [tag-content (slurp tag-file)]
        (mf/parse-file tag-content)))))


(def metadata-keys-tags (list :name :author :created :version :tags))
(def create-tag-file (partial mf/create-file metadata-keys-tags))

(defn- write-tag-object!
  [tag-name tag-object]
  (let [tag-file-path (format "%s/%s.md" config/jarvis-tag-directory tag-name)]
    (jda/write-jarvis-document! "tags" tag-file-path create-tag-file
                                (cs/lower-case tag-name) tag-object)))

(defn- create-tag-object
  "Create Tag from TagRequest"
  [tag-request]
  (let [now-isoformat (tf/unparse (tf/formatters :date-hour-minute-second) (tc/now))]
    (assoc tag-request :created now-isoformat :version config/jarvis-tag-version)))


(s/defn valid-tag?
  "TODO: Actually check the Tag object. Might want to consider using arity to be
  able to check just the Tag."
  [tag-name :- s/Str tag-to-check :- Tag]
  (= tag-name (:name tag-to-check)))


(s/defn put-tag! :- Tag
  [tag-name :- s/Str tag-updated :- Tag]
  (write-tag-object! tag-name tag-updated))


(s/defn post-tag! :- Tag
  "Takes a TagRequest converts to a Tag which is written to the filesystem in the
  tag file format."
  [tag-request :- TagRequest]
  (put-tag! (:name tag-request) (create-tag-object tag-request)))


(s/defn migrate-tag! :- Tag
  "Migrate the previous tag object from a former schema to the new schema"
  [tag-name :- s/Str tag-to-migrate :- TagPrev]
  (let [tag-object (assoc tag-to-migrate :name tag-name
                          :version config/jarvis-tag-version)]
    (write-tag-object! tag-name tag-object)))
