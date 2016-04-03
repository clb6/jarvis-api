(ns jarvis-api.data_access
  (:require [jarvis-api.elasticsearch :as jes]
            [clojure.java.io :as io]
            [clojure.tools.logging :as log]))


(defn- delete-jarvis-document!
  ; TODO: Better error checking and returning. The delete-file throws exceptions.
  [document-type file-path document-id]
  (jes/delete-jarvis-document document-type document-id)
  (io/delete-file file-path))

(defn- write-jarvis-document-unsafe!
  [document-type file-path create-file-representation document-id document]
  (if-let [document (jes/put-jarvis-document document-type document-id document)]
    (do (spit file-path (create-file-representation document))
        document)))

(defn write-jarvis-document!
  [document-type file-path create-file-representation document-id document]
  (let [document-prev (jes/get-jarvis-document document-type document-id)]
    (letfn [(rollback []
              (try
                (if document-prev
                  (write-jarvis-document-unsafe! document-type file-path
                                                      create-file-representation
                                                      document-id
                                                      document-prev)
                  (delete-jarvis-document! document-type file-path document-id))
                (catch Exception e
                  (log/error (str "Error rolling back: " (.getMessage e))))))]
      (try
        (if-let [document-written (write-jarvis-document-unsafe! document-type file-path
                                                                 create-file-representation
                                                                 document-id
                                                                 document)]
          document-written
          (do (log/error (str "Document failed to write: " document-id))
              (rollback)))
      (catch Exception e
        (log/error (str "Error writing document: " (.getMessage e)))
        ; Try to rollback changes
        (rollback)
        )))))

(defn get-jarvis-document!
  [document-type document-id]
  (jes/get-jarvis-document document-type document-id))


(def add-query-criteria-tag-name (partial jes/add-query-criteria-wildcard :name))
(def add-query-criteria-tags (partial jes/add-query-criteria-wildcard :tags))

(def add-query-criteria-body (partial jes/add-query-criteria-match :body))

(def query-tags (partial jes/query-jarvis-documents "tags" { "name" "asc" }))
(def query-log-entries (partial jes/query-jarvis-documents "logentries"
                               { "occurred" "desc" }))

(defn get-hits-from-query
  [query-result]
  (first query-result))

(defn get-total-hits-from-query
  [query-result]
  (second query-result))
