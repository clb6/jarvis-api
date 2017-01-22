(ns jarvis-api.resources.logentries
  (:require [clojure.string :as cs]
            [clj-time.core :as tc]
            [schema.core :as s]
            [jarvis-api.schemas :refer [LogEntryObject LogEntryRequest]]
            [jarvis-api.config :as config]
            [jarvis-api.data-accessing :as jda]
            [jarvis-api.data_access.queryhelp :as jqh]
            [jarvis-api.database.elasticsearch :as jes]
            [jarvis-api.database.redis :as jre]
            [jarvis-api.util :as util]))


(defn query-log-entries
  "Returns { :items [LogEntryObject] :total Long } if there are no hits then :items is
  an empty list"
  [tags searchterm from]
  (let [query-criterias (jqh/add-query-criteria-tags tags)
        query-criterias (jqh/add-query-criteria-body searchterm query-criterias)
        query-result (jqh/query-log-entries query-criterias from)]
    { :items (jqh/get-hits-from-query query-result)
      :total (jqh/get-total-hits-from-query query-result) }))


(def get-logentry-elasticsearch! (partial jes/get-jarvis-document "logentries"))
(def put-logentry-elasticsearch! (partial jes/put-jarvis-document "logentries"))
(def delete-logentry-elasticsearch! (partial jes/delete-jarvis-document "logentries"))

(defn get-logentry-id
  [logentry-object]
  (:id logentry-object))


(def write-logentry! (jda/create-write-func put-logentry-elasticsearch!
                                            jre/add-logentry-to-event!))
(def remove-logentry! (jda/create-remove-func delete-logentry-elasticsearch!
                                              jre/remove-logentry-from-event!))
(def rollback-logentry! (jda/create-rollback-func write-logentry!
                                                  remove-logentry!))
(def write-logentry-reliably! (jda/create-write-reliably-func
                                get-logentry-elasticsearch!
                                write-logentry!
                                rollback-logentry!
                                get-logentry-id))


(s/defn get-log-entry! :- LogEntryObject
  [id :- s/Int]
  (get-logentry-elasticsearch! id))

(s/defn log-entry-exists?
  [id :- s/Int]
  ((comp not nil?) (get-log-entry! id)))


(defn- generate-log-id
  [created]
  (tc/in-seconds (tc/interval (tc/epoch) created)))

(defn- create-log-entry-object
  "Creates a full LogEntry meaning that fields that are considered optional in
  the request are added into the object."
  [log-entry-request id created-isoformat event-id]
  (let [modified-isoformat (util/create-timestamp-isoformat)
        log-entry-object (assoc log-entry-request
                                :id id
                                :created created-isoformat
                                :modified modified-isoformat
                                :version config/jarvis-log-entry-version
                                :event event-id)]
    (reduce (fn [target-map k] (if (not (contains? target-map k))
                                 (assoc target-map k nil)
                                 target-map))
            log-entry-object
            [:parent :event :todo])))


(s/defn post-log-entry! :- LogEntryObject
  "Post a new log entry where new entries are appended or if this is a migration
  then the old version is updated"
  [log-entry-request :- LogEntryRequest event-id :- s/Str]
  (let [created (tc/now)
        created-isoformat (or (:created log-entry-request)
                              (util/create-timestamp-isoformat created))
        id (int (or (:id log-entry-request) (generate-log-id created)))
        log-entry-object (create-log-entry-object log-entry-request id created-isoformat
                                                  event-id)]
    (write-logentry-reliably! log-entry-object)))


(s/defn update-log-entry! :- LogEntryObject
  [log-entry-object :- LogEntryObject log-entry-request :- LogEntryRequest]
  (let [updated-log-entry-object (merge log-entry-object log-entry-request)
        updated-log-entry-object (assoc updated-log-entry-object
                                        :modified (util/create-timestamp-isoformat))]
    (write-logentry-reliably! updated-log-entry-object)))
