(ns jarvis-api.resources.datasummary
  (:require [jarvis-api.elasticsearch :as es]))


(defn create-data-summary
  [data-type]
  ; Count Elasticsearch
  ; Count Files
  { :data-type data-type
   :status :ok
   :count (es/count-jarvis-documents data-type)})
