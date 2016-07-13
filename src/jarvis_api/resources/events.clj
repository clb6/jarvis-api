(ns jarvis-api.resources.events
  (:require [clojure.string :as cs]
            [schema.core :as s]
            [jarvis-api.schemas :refer [EventObject EventRequest EventArtifact
                                        EventMixin]]
            [jarvis-api.data_access.events :as jda]
            [jarvis-api.data_access.queryhelp :as jqh]
            [jarvis-api.util :as util]))


(defn query-events
  "Returns { :items [EventObjects] :total Long } if there are no hits then :items is
  an empty list"
  [category weight from]
  (let [query-criterias (jqh/add-query-criteria-category category)
        query-criterias (jqh/add-query-criteria-weight weight query-criterias)
        query-result (jqh/query-events query-criterias from)]
    { :items (jqh/get-hits-from-query query-result)
      :total (jqh/get-total-hits-from-query query-result) }))

(s/defn get-event! :- EventMixin
  [event-id :- String]
  (jda/get-event event-id))


(s/defn post-event! :- EventMixin
  [event-request :- EventRequest]
  (let [event-id (or (:eventId event-request) (util/generate-uuid))
        created-isoformat (or (:created event-request)
                              (util/create-timestamp-isoformat))
        occurred-isoformat (or (:occurred event-request) created-isoformat)
        event-object (dissoc event-request :artifacts)
        event-object (assoc event-object :eventId event-id
                            :created created-isoformat :occurred occurred-isoformat
                            :location (:location event-request))]
    (jda/write-event! event-object (:artifacts event-request))))


(s/defn update-event! :- EventMixin
  [event-mixin :- EventMixin event-request :- EventRequest]
  (let [event-object (dissoc event-mixin :artifacts :logEntries)
        updated-event-object (merge event-object (dissoc event-request :artifacts))]
    (jda/write-event! updated-event-object (:artifacts event-request))))
