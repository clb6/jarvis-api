(ns jarvis-api.data_access.events
  (:require [schema.core :as s]
            [jarvis-api.schemas :refer [EventObject EventArtifact EventMixin]]
            [taoensso.carmine :as car]
            [taoensso.timbre :as timbre :refer [warn error]]
            ))


(s/defn make-event-from-object :- EventMixin
  "Create event from event object

  Create the event from the respective event object. If event object is
  nil then nil is returned."
  [get-log-entry-ids-func get-artifacts-func event-object :- EventObject]
  (if (not (nil? event-object))
    (let [event-id (:eventId event-object)
          artifacts (get-artifacts-func event-id)
          log-entries (get-log-entry-ids-func event-id)]
      ; Decided not to validate this value because this method is used
      ; for reads. Should pass back the bad events so they can be examined
      ; at higher levels.
      (assoc event-object :artifacts artifacts :logEntries log-entries)))
  )


(s/defn get-event :- EventMixin
  [get-event-object-func make-event-func event-id :- String]
  (let [event-object (get-event-object-func event-id)]
    (make-event-func event-object)))


(s/defn write-event :- EventMixin
  "High order funciton to write an event

  Event object and associated artifacts are written together in a loose
  transaction. If a previous version of the event is provided, that is used to
  rollback. Upon a successful write, the new event is returned.

  There is no schema validation because the expectation is that the caller has
  done this."
  ([put-event-object-func update-artifacts-func make-event-func
    event-object :- EventObject artifacts :- [EventArtifact] event-prev :- EventMixin]
    (letfn [(handle-error []
              (if (nil? event-prev)
                (error "Failed to write event")
                (do
                  (warn "Failed to write event. Attempting to rollback")
                  (write-event put-event-object-func
                               update-artifacts-func
                               make-event-func
                               (dissoc event-prev :logEntries :artifacts)
                               (:artifacts event-prev) nil)
                  ; Do not want to give the false impression that the write went
                  ; smoothly for a rollback
                  nil)
                )
              )]

      (try
        (let [event-id (:eventId event-object)
              event-object-added (put-event-object-func event-id event-object)
              num-artifacts-added (update-artifacts-func event-id artifacts)
              artifacts-replace-failed? (not= (count artifacts) num-artifacts-added)
              ]

          ; Writing event object and artifacts is one transaction
          (if (or (nil? event-object-added) artifacts-replace-failed?)
            (handle-error)
            (make-event-func event-object-added)))
        (catch Exception e
               (error "Unexpected exception: " (.getMessage e))
               (handle-error))
        )
      ))

  ([put-event-object-func update-artifacts-func make-event-func
    event-object :- EventObject artifacts :- [EventArtifact]]
   (write-event put-event-object-func update-artifacts-func make-event-func
                event-object artifacts nil))
  )
