(ns jarvis-api.resources.logentries
  (:require [clojure.string :as cs]
            [clj-time.core :as tc]
            [schema.core :as s]
            [jarvis-api.schemas :refer [LogEntryObject LogEntryRequest]]
            [jarvis-api.config :as config]
            [jarvis-api.markdown_filer :as mf]
            [jarvis-api.data_access :as jda]
            [jarvis-api.util :as util]))


(defn query-log-entries
  "Returns { :items [LogEntryObject] :total Long } if there are no hits then :items is
  an empty list"
  [tags searchterm from]
  (let [query-criterias (jda/add-query-criteria-tags tags)
        query-criterias (jda/add-query-criteria-body searchterm query-criterias)
        query-result (jda/query-log-entries query-criterias from)]
    { :items (jda/get-hits-from-query query-result)
      :total (jda/get-total-hits-from-query query-result) }))

(s/defn get-log-entry! :- LogEntryObject
  [id :- BigInteger]
  (if-let [log-entry (jda/get-jarvis-document! "logentries" id)]
    (update-in log-entry [:id] biginteger)))

(s/defn log-entry-exists?
  [id :- BigInteger]
  ((comp not nil?) (get-log-entry! id)))


; Tried to use the `keys` method from the schema but the sort order is not
; predictable. Maybe use two separate vectors to construct the schema via zip.
(def metadata-keys-log-entries (list :id :author :created :modified :occurred
                                     :version :tags :parent :event :todo :setting))
(def create-log-entry-file (partial mf/create-file metadata-keys-log-entries))

(defn- generate-log-id
  [created]
  (tc/in-seconds (tc/interval (tc/epoch) created)))

(defn- write-log-entry-object!
  [id log-entry-object]
  (let [log-entry-path (format "%s/%s.md" config/jarvis-log-directory id)]
    (jda/write-jarvis-document! "logentries" log-entry-path create-log-entry-file
                                id log-entry-object)))

(defn- create-log-entry-object
  "Creates a full LogEntry meaning that fields that are considered optional in
  the request are added into the object."
  [log-entry-request id created-isoformat]
  (let [modified-isoformat (util/create-timestamp-isoformat)
        log-entry-object (assoc log-entry-request
                                :id id
                                :created created-isoformat
                                :modified modified-isoformat
                                :version config/jarvis-log-entry-version)]
    (reduce (fn [target-map k] (if (not (contains? target-map k))
                                 (assoc target-map k nil)
                                 target-map))
            log-entry-object
            [:parent :event :todo])))


(s/defn post-log-entry! :- LogEntryObject
  "Post a new log entry where new entries are appended or if this is a migration
  then the old version is updated"
  [log-entry-request :- LogEntryRequest]
  (let [created (tc/now)
        created-isoformat (or (:created log-entry-request)
                              (util/create-timestamp-isoformat created))
        id (or (:id log-entry-request) (generate-log-id created))
        log-entry-object (create-log-entry-object log-entry-request id created-isoformat)]
    (write-log-entry-object! id log-entry-object)))


(s/defn update-log-entry! :- LogEntryObject
  [log-entry-object :- LogEntryObject log-entry-request :- LogEntryRequest]
  (let [updated-log-entry-object (merge log-entry-object log-entry-request)
        updated-log-entry-object (assoc updated-log-entry-object
                                        :modified (util/create-timestamp-isoformat))]
    (write-log-entry-object! (:id updated-log-entry-object) updated-log-entry-object)))
