(ns jarvis-api.data_access
  (:require [jarvis-api.elasticsearch :as jes]
            [clojure.java.io :as io]))


(defn- delete-jarvis-document!
  ; TODO: Better error checking and returning. The delete-file throws exceptions.
  [document-type file-path document-id]
  (jes/delete-jarvis-document document-type document-id)
  (io/delete-file file-path))

(defn- write-jarvis-document-unsafe!
  [document-type file-path create-file-representation document-id document]
  (jes/put-jarvis-document document-type document-id document)
  (spit file-path (create-file-representation document))
  document)

(defn write-jarvis-document!
  [document-type file-path create-file-representation document-id document]
  (let [document-prev (jes/get-jarvis-document document-type document-id)]
    (try
      (write-jarvis-document-unsafe! document-type file-path
                                          create-file-representation
                                          document-id
                                          document)
      (catch Exception e
        (println (str "Error writing document: " (.getMessage e)))
        ; Try to rollback changes
        (try
          (if document-prev
            (write-jarvis-document-unsafe! document-type file-path
                                                create-file-representation
                                                document-id
                                                document-prev)
            (delete-jarvis-document! document-type file-path document-id))
          (catch Exception e
            (println (str "Error rolling back: " (.getMessage e)))))
        ; TODO: Remove this throw and handle better at higher level
        (throw e)))))
