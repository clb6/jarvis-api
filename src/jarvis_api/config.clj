(ns jarvis-api.config
  (:require [clojure.string :as cs]))


(def jarvis-root-directory (System/getenv "JARVIS_DIR_ROOT"))
(def jarvis-log-directory (cs/join "/" [jarvis-root-directory "LogEntries"]))
(def jarvis-images-directory (cs/join "/" [jarvis-root-directory "Images"]))
(def jarvis-tag-directory (cs/join "/" [jarvis-root-directory "Tags"]))

(def jarvis-tag-version "0.3.0")
(def jarvis-log-entry-version "0.7.0")

(def jarvis-elasticsearch-uri "http://elasticsearch.jarvis.home:9200")
; Versioned by breakfast food
(def jarvis-elasticsearch-index "jarvis-danish")
