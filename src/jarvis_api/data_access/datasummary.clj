(ns jarvis-api.data_access.datasummary
  (:require [clojurewerkz.elastisch.aggregation :as ea]
            [jarvis-api.database.elasticsearch :as jes]))

(defn get-created-range
  [document-type]
  (let [aggregation-query { :max_created (ea/max "created")
                            :min_created (ea/min "created") }
        result (jes/aggregate-jarvis-documents document-type aggregation-query)]
    (map (fn [agg-key] (get-in result [agg-key :value_as_string]))
         [:max_created :min_created])))

(def get-created-range-tags (partial get-created-range "tags"))
(def get-created-range-log-entries (partial get-created-range "logentries"))
(def get-created-range-events (partial get-created-range "events"))
