(ns jarvis-api.data_access.queryhelp
  (:require [jarvis-api.database.elasticsearch :as jes]))


(def add-query-criteria-tag-name (partial jes/add-query-criteria-query-string :name))
(def add-query-criteria-tags (partial jes/add-query-criteria-query-string :tags))

; Specifically for log entries
(def add-query-criteria-body (partial jes/add-query-criteria-match :body))

; Specifically for events
(def add-query-criteria-category (partial jes/add-query-criteria-match :category))
(def add-query-criteria-weight (partial jes/add-query-criteria-range-gte :weight))

(def query-tags (partial jes/query-documents! "tags" { "name" "asc" }))
(def query-log-entries (partial jes/query-documents! "logentries"
                               { "created" "desc" }))
(def query-events (partial jes/query-documents! "events" { "occurred" "desc" }))

(defn get-hits-from-query
  [query-result]
  (first query-result))

(defn get-total-hits-from-query
  [query-result]
  (second query-result))
