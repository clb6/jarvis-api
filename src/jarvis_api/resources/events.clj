(ns jarvis-api.resources.events
  (:require [clojure.string :as cs]
            [schema.core :as s]
            [jarvis-api.schemas :refer [EventObject EventRequest EventArtifact
                                        EventMixin]]
            [jarvis-api.data_access.events :as jda]
            [jarvis-api.data_access.queryhelp :as jqh]
            [jarvis-api.database.redis :as jredis]
            [jarvis-api.database.elasticsearch :as jelastic]
            [jarvis-api.util :as util]))


(def put-event-object! (partial jelastic/put-jarvis-document "events"))
(def make-event! (partial jda/make-event-from-object jredis/get-log-entry-ids
                         jredis/get-artifacts))
(def write-event! (partial jda/write-event put-event-object! jredis/update-artifacts
                           make-event!))


(defn query-events!
  "Returns { :items [EventObjects] :total Long } if there are no hits then :items is
  an empty list"
  [category weight searchterm from]
  (let [query-criterias (jqh/add-query-criteria-category category)
        query-criterias (jqh/add-query-criteria-weight weight query-criterias)
        query-criterias (jqh/add-query-criteria-description searchterm query-criterias)
        query-result (jqh/query-events query-criterias from)
        make-events! (partial map make-event!)
        ]
    { :items (make-events! (jqh/get-hits-from-query query-result))
      :total (jqh/get-total-hits-from-query query-result) }))


(s/defn get-event! :- EventMixin
  [event-id :- String]
  (let [get-event-object (partial jelastic/get-jarvis-document "events")]
    (jda/get-event get-event-object make-event! event-id)))


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

    (write-event! event-object (:artifacts event-request))))


(s/defn update-event! :- EventMixin
  [event-mixin :- EventMixin event-request :- EventRequest]
  (let [event-object (dissoc event-mixin :artifacts :logEntries)
        updated-event-object (merge event-object (dissoc event-request :artifacts))]

    (write-event! updated-event-object (:artifacts event-request))))
