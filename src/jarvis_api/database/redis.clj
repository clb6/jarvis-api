(ns jarvis-api.database.redis
  (:require [taoensso.carmine :as car :refer (wcar)]))


(def conn {:pool {} :spec {:host "127.0.0.1" :port 6379}})
(defmacro wcar* [& body] `(car/wcar conn ~@body))

