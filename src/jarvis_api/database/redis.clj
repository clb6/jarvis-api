(ns jarvis-api.database.redis
  (:require [taoensso.carmine :as car :refer (wcar)]
            [taoensso.timbre :as timbre :refer [warn]]
            [jarvis-api.config :as config]))


(def conn {:pool {} :spec { :host config/jarvis-redis-host
                            :port config/jarvis-redis-port}})
(defmacro wcar* [& body] `(car/wcar conn ~@body))

; Utils

(defn- create-key-event-logentries
  [event-id]
  (str "event:" event-id ":logs"))

(defn- create-key-event-artifacts
  [event-id]
  (str "event:" event-id ":artifacts"))

; Events

(defn get-logentry-ids-for-event!
  ""
  [event-id event-mixin]
  (let [redis-key (create-key-event-logentries event-id)
        ; All of these log entry ids are returned as strings, convert back to
        ; integers
        logentry-ids (map car/as-int (wcar* (car/smembers redis-key)))]
    (assoc event-mixin :logEntries logentry-ids)
    ))

(defn get-event-artifacts!
  [event-id event-mixin]
  (let [redis-key (create-key-event-artifacts event-id)
        artifacts (wcar* (car/smembers redis-key))]
    (assoc event-mixin :artifacts artifacts)
    ))


(defn remove-event-artifacts!
  [event-id event-mixin]
  (let [redis-key (create-key-event-artifacts event-id)
        num-removed (wcar* (car/del redis-key))]
    ; Removes are always true. I would have to do a get and compare against
    ; num-removed but that seems like a meaningless comparison
    (>= num-removed 0))
  )

(defn write-event-artifacts!
  [event-id event-mixin]
  ; Try to remove and move on no matter what because can't tell whether the
  ; removal failed vs there was nothing to remove
  (remove-event-artifacts! event-id event-mixin)
  (let [artifacts (:artifacts event-mixin)
        redis-key (create-key-event-artifacts event-id)
        num-added (reduce + (map #(wcar* (car/sadd redis-key %)) artifacts))]
    (if (= num-added (count artifacts))
      event-mixin)
    ))

; For log entries

(defn- update-event-relations!
  [redis-operation-func log-entry-object]
  (if-let [event-id (:event log-entry-object)]
    (let [redis-key (create-key-event-logentries event-id)]
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
    ; Removes are always true. I would have to do a get and compare against
    ; num-removed but that seems like a meaningless comparison
    (>= result 0))
  )
