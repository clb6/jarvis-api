(ns jarvis-api.resources.events
  (:require [clojure.string :as cs]
            [schema.core :as s]
            [jarvis-api.schemas :refer [EventObject EventRequest EventArtifact
                                        EventMixin]]
            [jarvis-api.data-accessing :as jda]
            [jarvis-api.data_access.queryhelp :as jqh]
            [jarvis-api.database.redis :as jre]
            [jarvis-api.database.elasticsearch :as jes]
            [jarvis-api.util :as util]))


(defn query-events!
  "Returns { :items [EventObjects] :total Long } if there are no hits then :items is
  an empty list"
  [category weight searchterm from]
  (let [query-criterias (jqh/add-query-criteria-category category)
        query-criterias (jqh/add-query-criteria-weight weight query-criterias)
        query-criterias (jqh/add-query-criteria-description searchterm query-criterias)
        query-result (jqh/query-events query-criterias from)
        retrieve-func! (jda/create-retrieve-func jre/get-event-artifacts!
                                                 jre/get-logentry-ids-for-event!)
        ]
    { :items (map (fn [event] (retrieve-func! (:eventId event) event))
                  (jqh/get-hits-from-query query-result))
      :total (jqh/get-total-hits-from-query query-result) }))


(def get-event-object-elasticsearch! (partial jes/get-jarvis-document "events"))
(def delete-event-object-elasticsearch! (partial jes/delete-jarvis-document "events"))
(defn put-event-elasticsearch!
  [event-id event-mixin]
  (let [put-func (partial jes/put-jarvis-document "events")
        event-object (dissoc event-mixin :artifacts :logEntries)]
    (if (put-func event-id event-object)
      event-mixin))
  )


(s/defn get-event! :- EventMixin
  [event-id :- String]
  (let [retrieve-func! (jda/create-retrieve-func get-event-object-elasticsearch!
                                                 jre/get-event-artifacts!
                                                 jre/get-logentry-ids-for-event!)]
    (retrieve-func! event-id {})))


(def write-event! (jda/create-write-func put-event-elasticsearch!
                                         jre/write-event-artifacts!))
(def remove-event! (jda/create-remove-func delete-event-object-elasticsearch!
                                           jre/remove-event-artifacts!))
(def rollback-event! (jda/create-rollback-func write-event!
                                               remove-event!))
(def write-event-reliably! (jda/create-write-reliably-func
                             get-event!
                             write-event!
                             rollback-event!
                             (fn [event-mixin] (:eventId event-mixin))
                             ))


(s/defn post-event! :- EventMixin
  [event-request :- EventRequest]
  (let [event-id (or (:eventId event-request) (util/generate-uuid))
        created-isoformat (or (:created event-request)
                              (util/create-timestamp-isoformat))
        occurred-isoformat (or (:occurred event-request) created-isoformat)
        event-object (assoc event-request :eventId event-id
                            :created created-isoformat :occurred occurred-isoformat
                            :location (:location event-request))]

    (write-event-reliably! event-object)
    ))


(s/defn update-event! :- EventMixin
  [event-mixin :- EventMixin event-request :- EventRequest]
  (let [event-object (dissoc event-mixin :artifacts :logEntries)
        updated-event (merge event-object event-request)]

    (write-event-reliably! updated-event)
    ))
