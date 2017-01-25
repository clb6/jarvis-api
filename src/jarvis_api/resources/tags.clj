(ns jarvis-api.resources.tags
  (:require [clojure.string :as cs]
            [schema.core :as s]
            [jarvis-api.schemas :refer [TagObject TagRequest]]
            [jarvis-api.config :as config]
            [jarvis-api.data-accessing :as jda]
            [jarvis-api.data_access.queryhelp :as jqh]
            [jarvis-api.database.elasticsearch :as jes]
            [jarvis-api.util :as util]
            [jarvis-api.links :as jl]))


(defn query-tags
  [tag-name tags from]
  (let [query-criterias (jqh/add-query-criteria-tag-name tag-name)
        query-criterias (jqh/add-query-criteria-tags tags query-criterias)
        query-result (jqh/query-tags query-criterias from)]
    { :items (jqh/get-hits-from-query query-result)
      :total (jqh/get-total-hits-from-query query-result) }))


(s/defn get-tag! :- TagObject
  "Returns web response where it will return a tag object if a tag is found"
  [tag-name :- String]
  (let [get-tag-elasticsearch! (partial jes/get-jarvis-document "tags")]
    (get-tag-elasticsearch! (cs/lower-case tag-name))
  ))


(defn tag-exists?
  "Case-insensitive check of whether a tag already exists"
  [tag-name]
  ((comp not nil?) (get-tag! tag-name))
  )


(s/defn filter-tag-names-missing :- [s/Str]
  "Given a list of tag names, returns the list of tag names that are missing"
  [tag-names :- [s/Str]]
  (filter #(not (tag-exists? %1)) tag-names)
  )


(s/defn make-tag-object :- TagObject
  "Create Tag from TagRequest"
  [tag-request :- TagRequest]
  (let [modified-isoformat (util/create-timestamp-isoformat)
        ; Allow created timestamp to be passed-in. The use case is migrations.
        created-isoformat (if (contains? tag-request :created)
                            (:created tag-request)
                            modified-isoformat)]
    (assoc tag-request :created created-isoformat :modified modified-isoformat
           :version config/jarvis-tag-version))
  )


(s/defn valid-tag?
  [tag-name :- s/Str tag-request :- TagRequest]
  (= (cs/lower-case tag-name) (cs/lower-case (:name tag-request)))
  )


(defn- create-write-tag-reliably-func
  []
  (letfn [(get-tag-id
            [tag-object]
            (cs/lower-case (:name tag-object)))]
    (let [put-tag-elasticsearch! (partial jes/put-jarvis-document "tags")
          delete-tag-elasticsearch! (partial jes/delete-jarvis-document "tags")

          write-tag! (jda/create-write-func put-tag-elasticsearch!)
          remove-tag! (jda/create-remove-func delete-tag-elasticsearch!)
          rollback-tag! (jda/create-rollback-func write-tag!
                                                  remove-tag!)]
      (jda/create-write-reliably-func get-tag! write-tag! rollback-tag! get-tag-id)
      )))

(def write-tag-reliably! (create-write-tag-reliably-func))


(s/defn post-tag! :- TagObject
  "Takes a TagRequest converts to a Tag which is written to the filesystem in the
  tag file format."
  [tag-request :- TagRequest]
  (let [tag-object (make-tag-object tag-request)]
    (write-tag-reliably! tag-object))
  )


(s/defn update-tag! :- TagObject
  [tag-object :- TagObject tag-request :- TagRequest]
  (let [updated-tag-object (merge tag-object tag-request)
        updated-tag-object (assoc updated-tag-object
                                  :modified (util/create-timestamp-isoformat))]
    (write-tag-reliably! updated-tag-object))
  )
