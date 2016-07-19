(ns jarvis-api.database.redis
  (:require [taoensso.carmine :as car :refer (wcar)]
            [jarvis-api.config :as config]))


(def conn {:pool {} :spec { :host config/jarvis-redis-host
                            :port config/jarvis-redis-port}})
(defmacro wcar* [& body] `(car/wcar conn ~@body))

