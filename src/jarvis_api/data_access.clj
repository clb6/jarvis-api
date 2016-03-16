(ns jarvis-api.data_access
  (:require [jarvis-api.elasticsearch :as jes]))

(defn write-jarvis-document!
  [document-type file-path document-id document create-file-representation]
  ; TODO: Handle unexpected errors and parseable errors
  (jes/put-jarvis-document document-type document-id document)
  (spit file-path (create-file-representation document))
  document)
