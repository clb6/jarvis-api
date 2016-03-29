(ns jarvis-api.elasticsearch
  (:require [clojurewerkz.elastisch.rest  :as esr]
            [clojurewerkz.elastisch.rest.document :as esd]
            [clojurewerkz.elastisch.query :as esq]
            [jarvis-api.config :as config]))

; Had to force the document-id to be a string because Elasticsearch complains
; otherwise.

(defn put-jarvis-document
  "Example response: {:_index jarvis, :_type tags, :_id forecasting, :_version 1,
  :_shards {:total 2, :successful 1, :failed 0}, :created true}"
  [document-type document-id document]
  (let [conn (esr/connect config/jarvis-elasticsearch-uri)
        document-id (str document-id)
        version-prev (:_version (esd/get conn "jarvis" document-type document-id))
        response (esd/put conn "jarvis" document-type document-id document)]
    ; Handle both when there hasn't been an existing version and when a new
    ; version has been pushed.
    (if (or (:created response) (> (:_version response) version-prev))
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

(defn count-jarvis-documents
  [document-type]
  (let [conn (esr/connect config/jarvis-elasticsearch-uri)]
    (:count (esd/count conn "jarvis" document-type))))


(defn add-query-criteria-wildcard
  "Helper method to build Elasticsearch query request where this method adds a
  wildcard query to a list of queries that is intended for a bool compound query."
  ([field value]
  (add-query-criteria-wildcard field value []))
  ([field value query-array]
   (if-not (or (nil? value) (empty? value))
     (conj query-array (esq/wildcard { field (str "*" value "*") }))
     query-array)))

(defn query-jarvis-documents
  "Constrained Elasticsearch querying.

  Returns [total hits]

  TODO: How to enable a diverse set of querying? Right now querying is hardcoded
  to be `bool` and `must`."
  [document-type sort-request query-criterias from]
  (let [conn (esr/connect config/jarvis-elasticsearch-uri)
        query-request (esq/bool { :must query-criterias })
        result (esd/search conn "jarvis" document-type :query query-request
                           :sort sort-request :from from)]
    [(map :_source (:hits (:hits result))) (get-in result [:hits :total])]))
