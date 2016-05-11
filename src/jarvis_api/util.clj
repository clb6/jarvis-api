(ns jarvis-api.util
  (:require [clj-time.core :as tc]
            [clj-time.format :as tf]))


(defn set-field-default-maybe
  "Set a specified key with a default value if it does not exist already"
  [jarvis-resource metadata-key default]
  (if (not (contains? jarvis-resource metadata-key))
    (assoc jarvis-resource metadata-key default)
    jarvis-resource))


(defn create-timestamp-isoformat
  "Create a string representation in ISO-8601 of now"
  ([] (create-timestamp-isoformat (tc/now)))
  ([timestamp] (tf/unparse (tf/formatters :date-hour-minute-second) timestamp)))
