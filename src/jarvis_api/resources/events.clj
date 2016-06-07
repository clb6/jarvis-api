(ns jarvis-api.resources.events
  (:require [clojure.string :as cs]
            [schema.core :as s]
            [jarvis-api.schemas :refer [EventObject EventRequest]]
            [jarvis-api.data_access :as jda]
            [jarvis-api.util :as util]))


(defn query-events
  "Returns { :items [EventObjects] :total Long } if there are no hits then :items is
  an empty list"
  [category from]
  (let [query-criterias (jda/add-query-criteria-category category)
        query-result (jda/query-events query-criterias from)]
    { :items (jda/get-hits-from-query query-result)
      :total (jda/get-total-hits-from-query query-result) }))

(s/defn get-event! :- EventObject
  [event-id :- String]
  (jda/get-jarvis-document! "events" event-id))


(s/defn post-event! :- EventObject
  [event-request :- EventRequest]
  (let [created-isoformat (util/create-timestamp-isoformat)
        occurred-isoformat (or (:occurred event-request) created-isoformat)
        event-object (assoc event-request :eventId (util/generate-uuid)
                            :created created-isoformat :occurred occurred-isoformat
                            :location (:location event-request))]
    (jda/write-jarvis-document! "events" (:eventId event-object) event-object)))
