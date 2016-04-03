(ns jarvis-api.links
  (:require [org.bovinegenius.exploding-fish :as ef]
            [jarvis-api.data_access :as jda]))


(def default-page-size 10)

(defn- construct-new-uri-string
  [fully-qualified-uri from-to-update]
  (str (ef/param fully-qualified-uri "from" from-to-update)))

(defn- calc-prev-from
  [query-result current-from fully-qualified-uri]
  { :href (construct-new-uri-string fully-qualified-uri current-from)
    :rel "prev" })

(defn- calc-next-from
  [query-result current-from fully-qualified-uri]
  (let [total-hits (jda/get-total-hits-from-query query-result)
        bump (min (- total-hits current-from) default-page-size)
        next-from (+ bump current-from)]
    (if (< next-from total-hits)
      { :href (construct-new-uri-string fully-qualified-uri next-from)
        :rel "next" })))

(defn generate-query-links
  [query-result from fully-qualified-uri]
  (remove nil? (map #(%1 query-result from fully-qualified-uri)
                    [calc-next-from calc-prev-from])))

