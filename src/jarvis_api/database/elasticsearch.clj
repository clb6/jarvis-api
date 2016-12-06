(ns jarvis-api.database.elasticsearch
  (:require [clojurewerkz.elastisch.rest  :as esr]
            [clojurewerkz.elastisch.rest.document :as esd]
            [clojurewerkz.elastisch.rest.index :as esi]
            [clojurewerkz.elastisch.rest.response :refer [aggregations-from]]
            [clojurewerkz.elastisch.query :as esq]
            [taoensso.timbre :as timbre :refer [info error]]
            [jarvis-api.config :as config]))

; Had to force the document-id to be a string because Elasticsearch complains
; otherwise.

(defn put-jarvis-document
  "Example response: {:_index jarvis, :_type tags, :_id forecasting, :_version 1,
  :_shards {:total 2, :successful 1, :failed 0}, :created true}"
  [document-type document-id document]
  (let [conn (esr/connect config/jarvis-elasticsearch-uri)
        document-id (str document-id)
        version-prev (:_version (esd/get conn config/jarvis-elasticsearch-index
                                         document-type document-id))
        response (esd/put conn config/jarvis-elasticsearch-index document-type
                          document-id document)]
    ; Handle both when there hasn't been an existing version and when a new
    ; version has been pushed.
    (if (or (:created response) (> (:_version response) version-prev))
      document)))

(defn get-jarvis-document
  [document-type document-id]
  (let [conn (esr/connect config/jarvis-elasticsearch-uri)
        response (esd/get conn config/jarvis-elasticsearch-index document-type
                          (str document-id))]
    (if (:found response)
      (:_source response))))

(defn delete-jarvis-document
  [document-type document-id]
  (let [conn (esr/connect config/jarvis-elasticsearch-uri)]
    (esd/delete conn config/jarvis-elasticsearch-index document-type
                (str document-id))))

(defn count-jarvis-documents
  [document-type]
  (let [conn (esr/connect config/jarvis-elasticsearch-uri)]
    (:count (esd/count conn config/jarvis-elasticsearch-index document-type))))


(defn- add-query-criteria
  "Build up query criteria

  This a higher order to be used in more specific cases to build up a complex
  query criteria used to pass into an Elasticsearch query request.  The output 
  is a list of query criterias which can then be passed into subsequent calls to
  the same function.  You are creating a bool compound query.
  
  The signature of the query-criteria-constructor function is:
    
    some-func [field value] -> Elasticsearch query criteria
  "
  ([query-criteria-constructor field value]
   (add-query-criteria query-criteria-constructor field value []))

  ([query-criteria-constructor field value query-array]
   (if (nil? value)
     query-array
     (conj query-array (query-criteria-constructor field value))))
  )

; NOTE: Went from "wildcard" to "query-string" because of issue with case
; sensitivity of the "wildcard"
(def add-query-criteria-query-string (partial add-query-criteria
                                              (fn[field value]
                                                (esq/query-string
                                                  { "query" (str (name field)
                                                                 ":*" value "*") }))))

(def add-query-criteria-match (partial add-query-criteria
                                       (fn[field value]
                                         (esq/match field value))))

(def add-query-criteria-range-gte (partial add-query-criteria
                                           (fn[field value]
                                             (esq/range field { "gte"  value }))))

(defn query-documents!
  "Constrained Elasticsearch query

  Returns [hits total]

  TODO: How to enable a diverse set of querying? Right now querying is hardcoded
  to be `bool` and `must`."
  ([{:keys [uri index-name]} document-type sort-criteria query-criterias from]
   (let [conn (esr/connect uri)
         query-request (esq/bool { :must query-criterias })
         result (esd/search conn index-name document-type
                            :query query-request :sort sort-criteria :from from)]
     [(map :_source (:hits (:hits result))) (get-in result [:hits :total])]))

  ([document-type sort-criteria query-criterias from]
   (query-documents! { :uri config/jarvis-elasticsearch-uri
                      :index-name config/jarvis-elasticsearch-index }
                    document-type sort-criteria query-criterias from))
  )


(defn aggregate-jarvis-documents
  [document-type aggregation-query]
  (let [conn (esr/connect config/jarvis-elasticsearch-uri)
        ; :search_type "count" is used so that "hits" are not returned
        result (esd/search conn config/jarvis-elasticsearch-index document-type
                           :aggregations aggregation-query :query (esq/match-all)
                           :search_type "count")]
    (aggregations-from result)))


(defn initialize!
  "Initialize Elasticsearch for Jarvis

  Mappings is a seq of vectors where each vector is more of tuple in the form of:
  
    [type-name properties-json]
  
  where type-name is a string and properties-json is a hashmap"
  [mappings]
  (let [conn (esr/connect config/jarvis-elasticsearch-uri)
        index-name config/jarvis-elasticsearch-index
        type-exists? (partial esi/type-exists? conn index-name)
        update-mapping (partial esi/update-mapping conn index-name)
        ]
    ; Make sure index exists
    (if (not (esi/exists? conn index-name))
      (do
        (esi/create conn index-name)
        (info "Elasticsearch created index: " index-name))
      )

    ; Make sure all mappings exist
    (letfn [(ensure-mapping [mapping]
              (let [type-name (first mapping)
                    type-properties (second mapping)]
                (if (not (type-exists? type-name))
                  (do
                    (if (:acknowledged
                          (update-mapping type-name { :mapping type-properties }))
                      (info "Elasticsearch created type: " type-name)
                      (error "Elasticsearch failed to create type: " type-name)
                      ))
                  )))]
      (dorun (map ensure-mapping mappings))
      )
    ))
