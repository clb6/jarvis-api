(ns jarvis-api.resources.datasummary
  (:require [jarvis-api.elasticsearch :as es]
            [jarvis-api.config :as config]
            [jarvis-api.data_access :as jda]))


(defn- get-data-type-file-seq
  [data-type]
  (let [data-directory (case data-type
                         :tags config/jarvis-tag-directory
                         :logentries config/jarvis-log-directory)
        data-directory-file (clojure.java.io/file data-directory)]
    (file-seq data-directory-file)))

(defn- count-data-type-files
  "Fetch the number of files associated with this data type. Must subtract 1
  because the result of `file-seq` also includes the top-level directory."
  [data-type]
  (- (count (get-data-type-file-seq data-type)) 1))

(defn- check-status
  [data-type doc-count]
  (if (= :events data-type)
    :ok
    (let [file-count (count-data-type-files data-type)]
      (if (= file-count doc-count)
        :ok
        :inconsistent))))


(defn generate-data-summary
  [data-type]
  (let [doc-count (es/count-jarvis-documents (name data-type))
        created-range (case data-type
                        :tags (jda/get-created-range-tags)
                        :logentries (jda/get-created-range-log-entries)
                        :events (jda/get-created-range-events)
                        nil)]
    { :data-type data-type
      :status (check-status data-type doc-count)
      :count doc-count
      :latest (first created-range)
      :oldest (last created-range)}))
