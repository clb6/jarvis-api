(ns jarvis-api.elasticsearch
  (:require [clojurewerkz.elastisch.rest  :as esr]
            [clojurewerkz.elastisch.rest.document :as esd]
            [jarvis-api.config :as config]))

; Had to force the document-id to be a string because Elasticsearch complains
; otherwise.

(defn put-jarvis-document
  [document-type document-id document]
  (let [conn (esr/connect config/jarvis-elasticsearch-uri)]
    (esd/put conn "jarvis" document-type (str document-id) document)))

(defn get-jarvis-document
  [document-type document-id]
  (let [conn (esr/connect config/jarvis-elasticsearch-uri)
        response (esd/get conn "jarvis" document-type (str document-id))]
    (if (:found response)
      (:_source response))))

(defn delete-jarvis-document
  [document-type document-id]
  (let [conn (esr/connect config/jarvis-elasticsearch-uri)]
    (esd/delete conn "jarvis" document-type (str document-id))))
