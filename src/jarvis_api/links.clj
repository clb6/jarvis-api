(ns jarvis-api.links
  (:require [clojure.string :refer [lower-case blank?]]
            [org.bovinegenius.exploding-fish :as ef]
            [schema.core :as s]
            [jarvis-api.schemas :refer [LogEntry Tag EventObject Event EventMixin]]))


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

(def construct-new-tag-uri (partial construct-new-jarvis-resource-uri "tags"))
(def construct-new-event-uri (partial construct-new-jarvis-resource-uri "events"))

(defn construct-new-log-entry-uri
  [fully-qualified-uri event-id log-entry-id]
  (str (assoc fully-qualified-uri :path (str "/events/" event-id "/logentries/"
                                             log-entry-id)
              :query nil))
  )

(defn- construct-log-entry-link
  [fully-qualified-uri rel event-id log-entry-id]
  { :title log-entry-id :rel rel
    :href (construct-new-log-entry-uri fully-qualified-uri event-id log-entry-id) }
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
  [fully-qualified-uri event-id log-entry]
  (let [parent (:parent log-entry)
        ; Parent field can have emtpy strings unfortunately
        ; TODO: Fix - parent might not be from the same event..right?
        parent-link (if (not (blank? parent))
                      (construct-log-entry-link fully-qualified-uri "parent"
                                                event-id parent))]
    (dissoc (assoc log-entry :parentLink parent-link) :parent)))

(defn- replace-event-with-link
  [fully-qualified-uri log-entry]
  (let [event-id (:event log-entry)
        event-link { :title event-id :rel "event"
                     :href (construct-new-event-uri event-id fully-qualified-uri) }]
    (dissoc (assoc log-entry :eventLink event-link) :event)))


(s/defn expand-log-entry
  ; REVIEW: Do I need this first version of the method since event should always
  ; be on a log entry?
  ([fully-qualified-uri event-id log-entry :- LogEntry]
   (->> log-entry
        (replace-parent-with-link fully-qualified-uri event-id)
        (replace-tags-with-links fully-qualified-uri)
        (replace-event-with-link fully-qualified-uri)))
  ([fully-qualified-uri log-entry :- LogEntry]
   (expand-log-entry fully-qualified-uri (:event log-entry) log-entry)))

(s/defn expand-tag
  [fully-qualified-uri tag :- Tag]
  (->> tag
      (replace-tags-with-links fully-qualified-uri)))

(s/defn expand-event :- Event
  [fully-qualified-uri event-mixin :- EventMixin]
  (let [log-entry-link-func (partial construct-log-entry-link fully-qualified-uri
                                     "log-entry" (:eventId event-mixin))
        log-entry-links (map log-entry-link-func (:logEntries event-mixin))
        artifact-links (:artifacts event-mixin)]
    (dissoc (assoc event-mixin :logEntryLinks log-entry-links :artifactLinks artifact-links)
            :logEntries :artifacts))
  )
