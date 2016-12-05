(ns jarvis-api.config
  (:require [clojure.string :as cs]
            [environ.core :refer [env]]))


; REVIEW: Should this configuration be in an external file? There doesn't seem to
; be a way of passing in the configuration file through ring or compojure-api.
; Ring applications take in args like `port` so there must be a way but not
; documented.

(def jarvis-data-version (env :jarvis-data-version))

(defn- versionize-name
  [some-name version]
  (cs/join "-" [some-name version]))

(def jarvis-root-directory (env :jarvis-dir-root))
(def jarvis-log-directory (cs/join "/" [jarvis-root-directory
                                        (versionize-name "LogEntries"
                                                         jarvis-data-version)]))
(def jarvis-images-directory (cs/join "/" [jarvis-root-directory "Images"]))
(def jarvis-tag-directory (cs/join "/" [jarvis-root-directory
                                        (versionize-name "Tags" jarvis-data-version)]))
(def elasticsearch-mappings-directory (cs/join "/" [jarvis-root-directory
                                                    "Elasticsearch/mappings"]))

(def jarvis-tag-version "0.3.0")
(def jarvis-log-entry-version "0.8.0")

(def jarvis-elasticsearch-uri (or (env :jarvis-elasticsearch)
                                  "http://elasticsearch.jarvis.home:9200"))
(def jarvis-elasticsearch-index (versionize-name "jarvis" jarvis-data-version))

(def jarvis-redis-host (or (env :jarvis-redis-host) "redis.jarvis.home"))
(def jarvis-redis-port (read-string (or (env :jarvis-redis-port) "6379")))
