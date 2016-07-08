(ns jarvis-api.links
  (:require [clojure.string :refer [lower-case blank?]]
            [org.bovinegenius.exploding-fish :as ef]
            [schema.core :as s]
            [jarvis-api.schemas :refer [LogEntry Tag EventObject Event]]
            [jarvis-api.data_access.events :as dae]))


(def default-page-size 10)

(defn- construct-new-uri-string
  [fully-qualified-uri from-to-update]
  (str (ef/param fully-qualified-uri "from" from-to-update)))

(defn- calc-prev-from
  [current-from fully-qualified-uri]
  (let [prev-from (- current-from default-page-size)]
    (if (>= prev-from 0)
      { :href (construct-new-uri-string fully-qualified-uri prev-from)
        :rel "prev" })))

(defn- calc-next-from
  [total-hits current-from fully-qualified-uri]
  (let [bump (min (- total-hits current-from) default-page-size)
        next-from (+ bump current-from)]
    (if (< next-from total-hits)
      { :href (construct-new-uri-string fully-qualified-uri next-from)
        :rel "next" })))

(defn generate-query-links
  [total-hits from fully-qualified-uri]
  (remove nil? (map #(%1 from fully-qualified-uri)
                    [ (partial calc-next-from total-hits)
                      calc-prev-from])))


(defn- construct-new-jarvis-resource-uri
  "TODO: Can do better path creation"
  [resource-type resource-id src-uri]
  (str (assoc src-uri :path (str "/" resource-type "/" resource-id)
              :query nil)))

; TODO: logentry uri must change to include event
(def construct-new-log-entry-uri (partial construct-new-jarvis-resource-uri "logentries"))
(def construct-new-tag-uri (partial construct-new-jarvis-resource-uri "tags"))
(def construct-new-event-uri (partial construct-new-jarvis-resource-uri "events"))

(defn- construct-log-entry-link
  [fully-qualified-uri rel event-id log-entry-id]
  { :title log-entry-id :rel rel
    :href (str (assoc fully-qualified-uri
                      :path (str "/events/" event-id "/logentries/" log-entry-id)
                      :query nil)) }
  )

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

(s/defn expand-tag
  [fully-qualified-uri tag :- Tag]
  (->> tag
      (replace-tags-with-links fully-qualified-uri)))

(s/defn expand-event :- Event
  [fully-qualified-uri event-object :- EventObject]
  (let [log-entry-ids (dae/get-log-entry-ids-by-event-id (:eventId event-object))
        log-entry-link-func (partial construct-log-entry-link fully-qualified-uri
                                     "log-entry" (:eventId event-object))
        log-entry-links (map log-entry-link-func log-entry-ids)]
    (assoc event-object :logEntryLinks log-entry-links)))
