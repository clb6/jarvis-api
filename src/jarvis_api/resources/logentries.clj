(ns jarvis-api.resources.logentries
  (:require [clj-time.core :as tc]
            [clj-time.format :as tf]
            [schema.core :as s]
            [jarvis-api.schemas :refer [LogEntry LogEntryRequest]]
            [jarvis-api.config :as config]
            [jarvis-api.markdown_filer :as mf]))


(s/defn get-log-entry! :- LogEntry
  "Return web response where if ok, returns a log entry object"
  [id :- String]
  (let [log-entry-path (format "%s/%s.md" config/jarvis-log-directory id)]
    (if (.exists (clojure.java.io/as-file log-entry-path))
      (mf/parse-file (slurp log-entry-path)))))


; Tried to use the `keys` method from the schema but the sort order is not
; predictable. Maybe use two separate vectors to construct the schema via zip.
(def metadata-keys-log-entries (list :author :created :occurred :version :tags
                                     :parent :todo :setting))
(def create-log-entry-file (partial mf/create-file metadata-keys-log-entries))

(defn- create-log-entry-object
  "Creates a full LogEntry meaning that fields that are considered optional in
  the request are added into the object."
  [created log-entry-request]
  (let [now-isoformat (tf/unparse (tf/formatters :date-hour-minute-second) created)
        log-entry-object (assoc log-entry-request :created now-isoformat
                                :version config/jarvis-log-entry-version)]
    (reduce (fn [target-map k] (if (not (contains? target-map k))
                                 (assoc target-map k nil)))
            log-entry-object
            [:parent :todo])))

(defn- generate-log-entry-path
  "The file name is  simply the epoch time of the created clj-time/datetime.

  Returns the full path"
  [created]
  (let [log-entry-name (tc/in-seconds (tc/interval (tc/epoch) created))]
    (format "%s/%s.md" config/jarvis-log-directory log-entry-name)))

(s/defn post-log-entry! :- LogEntry
  "Post a new log entry where new entries are appended."
  [log-entry-request :- LogEntryRequest]
  (let [created (tc/now)
        log-entry-path (generate-log-entry-path created)
        log-entry-object (create-log-entry-object created log-entry-request)]
    (spit log-entry-path (create-log-entry-file log-entry-object))
    log-entry-object))
