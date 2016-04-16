(ns jarvis-api.links
  (:require [clojure.string :refer [lower-case blank?]]
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


(defn- construct-new-jarvis-resource-uri
  "TODO: Can do better path creation"
  [resource-type resource-id src-uri]
  (str (assoc src-uri :path (str "/" resource-type "/" resource-id)
              :query nil)))

(defn- replace-tags-with-links
  [fully-qualified-uri jarvis-object]
  (letfn [(construct-tag-uri [tag-name]
            (construct-new-jarvis-resource-uri "tags" (lower-case tag-name)
                                               fully-qualified-uri))]
    (let [tag-names (:tags jarvis-object)
          tag-links (map (fn [tag-name] { :title tag-name
                                          :rel "tag"
                                          :href (construct-tag-uri tag-name)
                                         }) tag-names)]
      (dissoc (assoc jarvis-object :tagLinks tag-links) :tags))))

(defn- replace-parent-with-link
  [fully-qualified-uri log-entry]
  (let [parent (:parent log-entry)
        ; Parent field can have emtpy strings unfortunately
        parent-link (if (not (blank? parent))
                      { :title parent
                        :rel "parent"
                        :href (construct-new-jarvis-resource-uri "logentries" parent
                                                                  fully-qualified-uri)
                        })]
    (dissoc (assoc log-entry :parentLink parent-link) :parent)))

(s/defn expand-log-entry
  [fully-qualified-uri log-entry :- LogEntry]
  (->> log-entry
      (replace-parent-with-link fully-qualified-uri)
      (replace-tags-with-links fully-qualified-uri)))
