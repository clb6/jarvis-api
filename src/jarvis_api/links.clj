(ns jarvis-api.links
  (:require [clojure.string :refer [lower-case]]
            [org.bovinegenius.exploding-fish :as ef]
            [schema.core :as s]
            [jarvis-api.schemas :refer [LogEntry]]
            [jarvis-api.data_access :as jda]))


(def default-page-size 10)

(defn- construct-new-uri-string
  [fully-qualified-uri from-to-update]
  (str (ef/param fully-qualified-uri "from" from-to-update)))

(defn- calc-prev-from
  [query-result current-from fully-qualified-uri]
  (let [prev-from (- current-from default-page-size)]
    (if (>= prev-from 0)
      { :href (construct-new-uri-string fully-qualified-uri prev-from)
        :rel "prev" })))

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


(defn- replace-tags-with-links
  [fully-qualified-uri jarvis-object]
  (letfn [(construct-tag-uri [tag-name]
            ; TODO: Probably can do better here
            ; e.g. why is there a question mark at the end of the updated URL???
            (str (assoc (assoc fully-qualified-uri :path
                               (str "/tags/" (lower-case tag-name)))
                        :param nil)))]
    (let [tag-names (:tags jarvis-object)
          tag-links (map (fn [tag-name] { :title tag-name
                                          :rel "tag"
                                          :href (construct-tag-uri tag-name)
                                         }) tag-names)]
      (dissoc (assoc jarvis-object :tagLinks tag-links) :tags))))

(s/defn expand-log-entry
  [fully-qualified-uri log-entry :- LogEntry]
  (replace-tags-with-links fully-qualified-uri log-entry))
