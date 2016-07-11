(ns jarvis-api.data_access.events
  (:require [clojure.tools.logging :as log]
            [schema.core :as s]
            [jarvis-api.schemas :refer [EventObject EventArtifact EventMixin]]
            [taoensso.carmine :as car]
            [jarvis-api.database.redis :as dar]
            [jarvis-api.database.elasticsearch :as jes]))


(defn get-log-entry-ids-by-event-id
  [event-id]
  (let [redis-key (str "event:" event-id ":logs")]
    (dar/wcar* (car/smembers redis-key)))
  )


(defn- create-key-event-artifacts
  [event-id]
  (str "event:" event-id ":artifacts"))

(defn- create-event
  [event-object event-artifacts]
  (let [log-entries (get-log-entry-ids-by-event-id (:eventId event-object))]
    (assoc event-object :artifacts event-artifacts :logEntries log-entries))
  )

; TODO: Need recovery upon failure
(s/defn write-event! :- EventMixin
  [event-object :- EventObject event-artifacts :- [EventArtifact]]
  ; Assuming Carmine will convert complex data structures into byte strings
  ; using nippy
  (letfn [(add-artifact-link [event-artifact]
            (let [redis-key (create-key-event-artifacts (:eventId event-object))]
              (dar/wcar* (car/sadd redis-key event-artifact))))]
    (try
      (if-let [result (jes/put-jarvis-document "events"
                                               (:eventId event-object)
                                               event-object)]
        (let [num-artifacts-added (reduce + (map add-artifact-link event-artifacts))
              delta-artifacts (- (count event-artifacts) num-artifacts-added)]
          (if (== delta-artifacts 0)
            (create-event result event-artifacts)
            (log/error (str "Failed to add all artifact links: " delta-artifacts))
            ))
        (log/error "Failed to put event"))
      (catch Exception e
        (log/error "Should try to rollback if exists else delete"))
      )))

(s/defn get-event :- EventMixin
  [event-id]
  (let [event-object (jes/get-jarvis-document "events" event-id)
        redis-key (create-key-event-artifacts event-id)
        event-artifacts (dar/wcar* (car/smembers redis-key))]
    (create-event event-object event-artifacts)))
