(ns jarvis-api.data_access.common
  (:require [clojure.java.io :as io]
            [clojure.tools.logging :as log]
            [jarvis-api.database.elasticsearch :as jes]))


(defn delete-jarvis-document!
  ; TODO: Better error checking and returning. The delete-file throws exceptions.
  [document-type file-path document-id]
  (jes/delete-jarvis-document document-type document-id)
  (io/delete-file file-path))

(defn write-jarvis-document-unsafe!
  [document-type file-path create-file-representation document-id document]
  (if-let [document (jes/put-jarvis-document document-type document-id document)]
    (do (spit file-path (create-file-representation document))
        document)))

(defn get-jarvis-document!
  [document-type document-id]
  (jes/get-jarvis-document document-type document-id))
