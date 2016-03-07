(ns jarvis-api.resources.logentries
  (:require [clojure.string :as cs]
            [clj-time.core :as tc]
            [clj-time.format :as tf]
            [schema.core :as s]
            [jarvis-api.schemas :refer [LogEntry LogEntryRequest LogEntryPrev]]
            [jarvis-api.config :as config]
            [jarvis-api.markdown_filer :as mf]))


(defn log-entry-exists?
  [id]
  (let [log-entry-path (format "%s/%s.md" config/jarvis-log-directory id)]
    (.exists (clojure.java.io/as-file log-entry-path))))

(s/defn get-log-entry! :- LogEntry
  "Return web response where if ok, returns a log entry object"
  [id :- String]
  (let [log-entry-path (format "%s/%s.md" config/jarvis-log-directory id)]
    (if (.exists (clojure.java.io/as-file log-entry-path))
      (mf/parse-file (slurp log-entry-path)))))


; Tried to use the `keys` method from the schema but the sort order is not
; predictable. Maybe use two separate vectors to construct the schema via zip.
(def metadata-keys-log-entries (list :id :author :created :occurred :version :tags
                                     :parent :todo :setting))
(def create-log-entry-file (partial mf/create-file metadata-keys-log-entries))

(defn- generate-log-id
  [created]
  (tc/in-seconds (tc/interval (tc/epoch) created)))

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

(defn- generate-log-entry-path
  "The file name is  simply the epoch time of the created clj-time/datetime.

  Returns the full path"
  [created]
  (let [log-entry-name (generate-log-id created)]
    (format "%s/%s.md" config/jarvis-log-directory log-entry-name)))

(s/defn post-log-entry! :- LogEntry
  "Post a new log entry where new entries are appended."
  [log-entry-request :- LogEntryRequest]
  (let [created (tc/now)
        log-entry-path (generate-log-entry-path created)
        log-entry-object (create-log-entry-object created log-entry-request)]
    (spit log-entry-path (create-log-entry-file log-entry-object))
    log-entry-object))


(defn- migrate-log-entry-object
  "Take in any older version of a log entry object and update it"
  [id log-entry-to-migrate]
  (letfn [(set-field-default-maybe [log-entry metadata-key default]
            (if (not (contains? log-entry metadata-key))
              (assoc log-entry metadata-key default)
              log-entry))
          (add-id [log-entry]
            (set-field-default-maybe log-entry :id id))
          (add-occurred [log-entry]
            (set-field-default-maybe log-entry :occurred "1970-01-01T00:00:00"))
          (update-version [log-entry]
            (assoc log-entry :version config/jarvis-log-entry-version))
          (add-parent [log-entry]
            (set-field-default-maybe log-entry :parent nil))
          (add-todo [log-entry]
            (set-field-default-maybe log-entry :todo nil))
          (add-setting [log-entry]
            (set-field-default-maybe log-entry :setting "N/A"))]
    (-> log-entry-to-migrate
      add-id
      add-occurred
      update-version
      add-parent
      add-todo
      add-setting)))

(s/defn migrate-log-entry! :- LogEntry
  [id :- s/Str log-entry-to-migrate :- LogEntryPrev]
  (let [log-entry-file-path (format "%s/%s.md" config/jarvis-log-directory id)
        log-entry-object (migrate-log-entry-object id log-entry-to-migrate)]
    (spit log-entry-file-path (create-log-entry-file log-entry-object))
    log-entry-object))
