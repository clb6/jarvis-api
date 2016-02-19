(ns jarvis-api.resources.tags
  (:require [clojure.string :as cs]
            [clj-time.core :as tc]
            [clj-time.format :as tf]
            [ring.util.http-response :refer :all]
            [jarvis-api.config :as config]
            [jarvis-api.markdown_filer :as mf]))


(defn- fetch-tag-files!
  "Returns a list of all available tags as Java Files"
  []
  (filter #(.isFile %1) (file-seq (clojure.java.io/file config/jarvis-tag-directory))))

(defn- filter-tag-files-by-tag-name
  "Function extracts the Java File with a matching name which means matches by a
  case-insensitive version of the file name. Returns only the first match."
  [tag-name tag-files]
  (let [tag-name (clojure.string/lower-case tag-name)]
    (letfn [(tag-file-to-name [tag-file]
              ((comp clojure.string/lower-case
                     #(clojure.string/replace %1 #".md" ""))
               (.getName tag-file)))]
    (first (filter #(= tag-name (tag-file-to-name %1)) tag-files)))))

(defn- tag-exists?
  "Case-insensitive check of whether a tag already exists"
  [tag-name]
  ((comp not nil?) (filter-tag-files-by-tag-name tag-name (fetch-tag-files!))))

(defn- filter-tag-names-missing
  "Given a list of tag names, returns the list of tag names that are missing"
  [tag-names]
  (filter #(not (tag-exists? %1)) tag-names))

(defn get-tag-object!
  "Returns the map representation of a given tag name"
  [tag-name]
  (let [tag-file (filter-tag-files-by-tag-name tag-name (fetch-tag-files!))]
    (if tag-file
      (let [tag-content (slurp tag-file)]
        (mf/parse-file tag-content)))))


(defn- generate-tag-file-metadata
  [tag-object]
  (let [metadata-keys (list :author :created :version :tags)]
    (letfn [(get-metadata-value [mk]
              (let [value (get tag-object mk)]
                (if (= clojure.lang.PersistentVector (type value))
                  (cs/join ", " value)
                  value)))
            (generate-line [mk]
              (cs/join ": " (list (cs/capitalize (name mk)) (get-metadata-value mk))))]
      (cs/join "\n" (map generate-line metadata-keys)))))

(defn- generate-tag-file
  [tag-object]
  (cs/join "\n\n" (list (generate-tag-file-metadata tag-object)
                        (get tag-object :body))))

(defn- create-tag-object
  "Create Tag from TagRequest"
  [tag-request]
  (let [now-isoformat (tf/unparse (tf/formatters :date-hour-minute-second) (tc/now))]
    (dissoc (assoc tag-request :created now-isoformat :version config/jarvis-tag-version)
            :name)))

(defn post-tag!
  "Takes a TagRequest converts to a Tag which is written to the filesystem in the
  tag file format.

  Returns web response"
  [tag-request]
  (let [tag-name (get tag-request :name)
        tag-file-path (format "%s/%s.md" config/jarvis-tag-directory tag-name)]
    (if (tag-exists? tag-name)
      (conflict)
      (if (nil? (spit tag-file-path (generate-tag-file
                                      (create-tag-object tag-request))))
        (ok { :tags_missing (filter-tag-names-missing (:tags tag-request)) })))))

