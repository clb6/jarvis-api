(ns jarvis-api.database.redis
  (:require [taoensso.carmine :as car :refer (wcar)]
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
