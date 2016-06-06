(ns jarvis-api.resources.events
  (:require [clojure.string :as cs]
            [schema.core :as s]
            [jarvis-api.schemas :refer [EventObject EventRequest]]
            [jarvis-api.data_access :as jda]
            [jarvis-api.util :as util]))


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
