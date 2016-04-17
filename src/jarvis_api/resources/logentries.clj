(ns jarvis-api.resources.logentries
  (:require [clojure.string :as cs]
            [clj-time.core :as tc]
            [clj-time.format :as tf]
            [schema.core :as s]
            [jarvis-api.schemas :refer [LogEntry LogEntryRequest LogEntryPrev]]
            [jarvis-api.config :as config]
            [jarvis-api.markdown_filer :as mf]
            [jarvis-api.data_access :as jda]
            [jarvis-api.util :as util]))


(defn query-log-entries
  "Returns { :items [LogEntry] :total Long } if there are no hits then :items is
  an empty list"
  [tags searchterm from]
  (let [query-criterias (jda/add-query-criteria-tags tags)
        query-criterias (jda/add-query-criteria-body searchterm query-criterias)
        query-result (jda/query-log-entries query-criterias from)]
    { :items (jda/get-hits-from-query query-result)
      :total (jda/get-total-hits-from-query query-result) }))

(s/defn get-log-entry! :- LogEntry
  [id :- BigInteger]
  (if-let [log-entry (jda/get-jarvis-document! "logentries" id)]
    (update-in log-entry [:id] biginteger)))

(s/defn log-entry-exists?
  [id :- BigInteger]
  ((comp not nil?) (get-log-entry! id)))


; Tried to use the `keys` method from the schema but the sort order is not
; predictable. Maybe use two separate vectors to construct the schema via zip.
(def metadata-keys-log-entries (list :id :author :created :occurred :version :tags
                                     :parent :todo :setting))
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
  [created log-entry-request]
  (let [now-isoformat (tf/unparse (tf/formatters :date-hour-minute-second) created)
        log-entry-object (assoc log-entry-request
                                :id (generate-log-id created)
                                :created now-isoformat
                                :version config/jarvis-log-entry-version)]
    (reduce (fn [target-map k] (if (not (contains? target-map k))
                                 (assoc target-map k nil)
                                 target-map))
            log-entry-object
            [:parent :todo])))


(s/defn post-log-entry! :- LogEntry
  "Post a new log entry where new entries are appended."
  [log-entry-request :- LogEntryRequest]
  (let [created (tc/now)
        log-entry-object (create-log-entry-object created log-entry-request)]
    (write-log-entry-object! (generate-log-id created) log-entry-object)))


(s/defn valid-log-entry?
  "TODO: Actually check the LogEntry object."
  [id :- BigInteger log-entry-to-check :- LogEntry]
  (= id (:id log-entry-to-check)))


(s/defn put-log-entry!
  [id :- BigInteger log-entry-updated :- LogEntry]
  (write-log-entry-object! id log-entry-updated))


(defn- migrate-log-entry-object
  "Take in any older version of a log entry object and update it"
  [id log-entry-to-migrate]
  (letfn [(add-id [log-entry]
            (util/set-field-default-maybe log-entry :id id))
          (add-occurred [log-entry]
            (util/set-field-default-maybe log-entry :occurred "1970-01-01T00:00:00"))
          (update-version [log-entry]
            (assoc log-entry :version config/jarvis-log-entry-version))
          (add-parent [log-entry]
            (util/set-field-default-maybe log-entry :parent nil))
          (add-todo [log-entry]
            (util/set-field-default-maybe log-entry :todo nil))
          (add-setting [log-entry]
            (util/set-field-default-maybe log-entry :setting "N/A"))]
    (-> log-entry-to-migrate
      add-id
      add-occurred
      update-version
      add-parent
      add-todo
      add-setting)))

(s/defn migrate-log-entry! :- LogEntry
  [id :- BigInteger log-entry-to-migrate :- LogEntryPrev]
  (let [log-entry-object (migrate-log-entry-object id log-entry-to-migrate)]
    (write-log-entry-object! id log-entry-object)))
