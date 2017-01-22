(ns jarvis-api.database.redis
  (:require [taoensso.carmine :as car :refer (wcar)]
            [taoensso.timbre :as timbre :refer [warn]]
            [jarvis-api.config :as config]))


(def conn {:pool {} :spec { :host config/jarvis-redis-host
                            :port config/jarvis-redis-port}})
(defmacro wcar* [& body] `(car/wcar conn ~@body))

; Events

(defn get-log-entry-ids
  ""
  [event-id]
  (let [redis-key (str "event:" event-id ":logs")]
    ; All of these log entry ids are returned as strings, convert back to
    ; integers
    (map car/as-int (wcar* (car/smembers redis-key))))
  )

(defn create-key-event-artifacts
  [event-id]
  (str "event:" event-id ":artifacts"))

(defn get-artifacts
  [event-id]
  (let [redis-key (create-key-event-artifacts event-id)]
    (wcar* (car/smembers redis-key)))
  )

(defn update-artifacts
  "Update artifacts

  Means delete existing and add the new artifacts. Returns the number of artifacts
  that were added."
  [event-id artifacts]
  (let [redis-key (create-key-event-artifacts event-id)]
    (wcar* (car/del redis-key))
    (reduce + (map #(wcar* (car/sadd redis-key %)) artifacts))
    )
  )

; For log entries

(defn- update-event-relations!
  [redis-operation-func log-entry-object]
  (if-let [event-id (:event log-entry-object)]
    (let [redis-key (str "event:" event-id ":logs")]
      (wcar* (redis-operation-func redis-key (:id log-entry-object))))
    (warn (str "No event set for " (:id log-entry-object)))
    ))

(defn add-logentry-to-event!
  [logentry-id logentry-object]
  (let [add-func (partial update-event-relations! car/sadd)
        result (add-func logentry-object)]
    (if (> result 0)
      logentry-object))
  )

(defn remove-logentry-from-event!
  [logentry-id logentry-object]
  (let [remove-func (partial update-event-relations! car/srem)
        result (remove-func logentry-object)]
    (> result 0))
  )
