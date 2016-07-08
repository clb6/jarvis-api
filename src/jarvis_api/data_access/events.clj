(ns jarvis-api.data_access.events
  (:require [clojure.tools.logging :as log]
            [taoensso.carmine :as car]
            [jarvis-api.database.redis :as dar]))


(defn get-log-entry-ids-by-event-id
  [event-id]
  (let [redis-key (str "event:" event-id ":logs")]
    (dar/wcar* (car/smembers redis-key)))
  )
