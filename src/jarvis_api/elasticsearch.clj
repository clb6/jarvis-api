(ns jarvis-api.elasticsearch
  (:require [clojurewerkz.elastisch.rest  :as esr]
            [clojurewerkz.elastisch.rest.document :as esd]
            [jarvis-api.config :as config]))

; Had to force the document-id to be a string because Elasticsearch complains
; otherwise.

(defn put-jarvis-document
  "Example response: {:_index jarvis, :_type tags, :_id forecasting, :_version 1,
  :_shards {:total 2, :successful 1, :failed 0}, :created true}"
  [document-type document-id document]
  (let [conn (esr/connect config/jarvis-elasticsearch-uri)
        response (esd/put conn "jarvis" document-type (str document-id) document)]
    (if (:created response)
      document)))

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
