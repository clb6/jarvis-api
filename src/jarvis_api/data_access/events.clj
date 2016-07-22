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

(defn- make-event-object-to-event
  ([event-object event-artifacts]
   (let [log-entries (get-log-entry-ids-by-event-id (:eventId event-object))]
     (assoc event-object :artifacts event-artifacts :logEntries log-entries)))
  ([event-object]
   (let [redis-key (create-key-event-artifacts (:eventId event-object))
         event-artifacts (dar/wcar* (car/smembers redis-key))]
     (make-event-object-to-event event-object event-artifacts)))
  )

(s/defn make-event-objects-to-events :- [EventMixin]
  [event-objects :- [EventObject]]
  (map make-event-object-to-event event-objects))


(s/defn get-event :- EventMixin
  [event-id]
  (let [event-object (jes/get-jarvis-document "events" event-id)]
    (make-event-object-to-event event-object)))


(defn- just-write-event-mixin!
  [event-object event-artifacts]
  ;; This method is used to just simply write the event mixin components throwing
  ;; caution to the wind.
  ;; 
  ;; 1. Write the event object
  ;; 2. Verify that something got written
  ;; 3. Replace the event artifacts
  ;; 4. Verify by counting the artifacts written
  ;;
  ;; Returns an EventMixin if all the simple checks pass else returns nil

  (let [event-id (:eventId event-object)
        redis-key (create-key-event-artifacts event-id)]
    (if-let [result (jes/put-jarvis-document "events" event-id event-object)]
      ; Assuming Carmine will convert complex data structures into byte strings
      ; using nippy.
      ; In order to handle event artifact updates, the existing ones are wiped
      ; away first before adding the new ones.
      (letfn [(add-event-artifact [event-artifact]
                (dar/wcar* (car/sadd redis-key event-artifact)))
              (replace-event-artifacts []
                (dar/wcar* (car/del redis-key))
                (reduce + (map add-event-artifact event-artifacts)))]
        ; Verify that the proper number of artifacts got added
        (let [num-artifacts-added (replace-event-artifacts)
              delta-artifacts (- (count event-artifacts) num-artifacts-added)]
          (if (== delta-artifacts 0)
            (make-event-object-to-event result event-artifacts)
            (log/error (str "Failed to add all artifacts: " delta-artifacts))
            )))
      (log/error "Failed to put event")
      ))
  )

(s/defn write-event! :- EventMixin
  [event-object :- EventObject event-artifacts :- [EventArtifact]]
  (let [event-mixin-prev (get-event (:eventId event-object))
        event-object-prev (dissoc event-mixin-prev :logEntries :artifacts)
        event-artifacts-prev (:artifacts event-mixin-prev)]
    (letfn [(rollback []
              (if-let [result (just-write-event-mixin! event-object-prev
                                                       event-artifacts-prev)]
                (log/info "Rollback successful")
                (log/error "Rollback failed")))]
      (try
        (if-let [event-mixin (just-write-event-mixin! event-object event-artifacts)]
          event-mixin

          (if (nil? event-mixin-prev)
            (log/info "No previous event so nothing to rollback")
            (rollback)))
        (catch Exception e
               (rollback))
        ))))
