(ns jarvis-api.data_access.logentries
  (:require [clojure.tools.logging :as log]
            [taoensso.carmine :as car]
            [jarvis-api.config :as config]
            [jarvis-api.markdown_filer :as mf]
            [jarvis-api.database.elasticsearch :as jes]
            [jarvis-api.database.redis :as dar]
            [jarvis-api.data_access.common :as dac]))

; TODO: Right now "event" is optional. Very soon we want to require "event".
; TODO: Will need to recalc scores after adding log to event.

(defn- update-event-relations
  [redis-operation-func log-entry-object]
  (if-let [event-id (:event log-entry-object)]
    (let [redis-key (str "event:" event-id ":logs")]
      (dar/wcar* (redis-operation-func redis-key (:id log-entry-object))))
    (log/warn (str "No event set for " (:id log-entry-object)))
    ))

(def add-log-entry-object-to-event (partial update-event-relations car/sadd))
(def remove-log-entry-object-from-event (partial update-event-relations car/srem))


; Tried to use the `keys` method from the schema but the sort order is not
; predictable. Maybe use two separate vectors to construct the schema via zip.
(def metadata-keys-log-entries (list :id :author :created :modified
                                     :version :tags :parent :event :todo))
(def create-log-entry-file (partial mf/create-file metadata-keys-log-entries))


(defn- generate-log-entry-path
  [log-entry-object]
  (format "%s/%s.md" config/jarvis-log-directory (:id log-entry-object)))


(defn delete-log-entry-object!
  [log-entry-object]
  (dac/delete-jarvis-document! "logentries" (generate-log-entry-path log-entry-object)
                               (:id log-entry-object))
  (remove-log-entry-object-from-event log-entry-object)
  )

(defn write-log-entry-object-unsafe!
  [log-entry-object]
  (let [log-entry-path (generate-log-entry-path log-entry-object)
        log-entry-id (:id log-entry-object)]
    (dac/write-jarvis-document-unsafe! "logentries" log-entry-path create-log-entry-file
                                       log-entry-id log-entry-object)
    (add-log-entry-object-to-event log-entry-object)
    ; Must return this back. Expected.
    log-entry-object)
  )


(def get-log-entry-object! (partial dac/get-jarvis-document! "logentries"))

(defn write-log-entry-object!
  [log-entry-object]
  (let [log-entry-id (:id log-entry-object)
        leo-prev (get-log-entry-object! log-entry-id)]
    (letfn [(rollback []
              (try
                (if leo-prev
                  (write-log-entry-object-unsafe! leo-prev)
                  (delete-log-entry-object! leo-prev))
                (catch Exception e
                  (log/error (str "Error rolling back: " (.getMessage e))))))]
      (try
        (if-let [leo-written (write-log-entry-object-unsafe! log-entry-object)]
          leo-written
          (do (log/error (str "Log entry object failed to write: " log-entry-id))
              (rollback)))
      (catch Exception e
        (log/error (str "Error writing log entry object: " (.getMessage e)))
        ; Try to rollback changes
        (rollback)
        )))))
