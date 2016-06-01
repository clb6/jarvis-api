(ns jarvis-api.config
  (:require [clojure.string :as cs]))


(def jarvis-data-version (System/getenv "JARVIS_DATA_VERSION"))

(defn- versionize-name
  [some-name version]
  (cs/join "-" [some-name version]))

(def jarvis-root-directory (System/getenv "JARVIS_DIR_ROOT"))
(def jarvis-log-directory (cs/join "/" [jarvis-root-directory
                                        (versionize-name "LogEntries"
                                                         jarvis-data-version)]))
(def jarvis-images-directory (cs/join "/" [jarvis-root-directory "Images"]))
(def jarvis-tag-directory (cs/join "/" [jarvis-root-directory
                                        (versionize-name "Tags" jarvis-data-version)]))

(def jarvis-tag-version "0.3.0")
(def jarvis-log-entry-version "0.7.0")

(def jarvis-elasticsearch-uri (or (System/getenv "JARVIS_ELASTICSEARCH")
                                  "http://elasticsearch.jarvis.home:9200"))
(def jarvis-elasticsearch-index (versionize-name "jarvis" jarvis-data-version))
