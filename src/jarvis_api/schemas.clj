(ns jarvis-api.schemas
  (:require [schema.core :as s]))


(s/defschema Link { :href s/Str
                    :rel s/Str
                    (s/optional-key :title) s/Str })


(s/defschema LogEntryObject { :id s/Num
                              :author String
                              :created String
                              :modified s/Str
                              :version String
                              :tags [s/Str]
                              :parent (s/maybe s/Str)
                              :event (s/maybe s/Str)
                              :todo (s/maybe s/Str)
                              :body String })

(s/defschema LogEntryRequest  { (s/optional-key :id) s/Num
                                :author String
                                ; Optionally put created here for migration purpose
                                (s/optional-key :created) s/Str
                                :tags [s/Str]
                                (s/optional-key :parent) (s/maybe s/Str)
                                (s/optional-key :todo) (s/maybe s/Str)
                                :body String })

(s/defschema LogEntry (dissoc (merge LogEntryObject { :tagLinks [Link]
                                                      :parentLink (s/maybe Link)
                                                      :eventLink Link })
                              :tags :parent :event))


(s/defschema TagObject { :name s/Str
                         :author s/Str
                         :created s/Str
                         :modified s/Str
                         :version s/Str
                         :tags [s/Str]
                         :body s/Str })

(s/defschema TagRequest { :name s/Str
                          :author s/Str
                          ; Optionally put created here for migration purpose
                          (s/optional-key :created) s/Str
                          :tags [s/Str]
                          :body s/Str })

(s/defschema Tag (dissoc (merge TagObject { :tagLinks [Link] }) :tags))


(s/defschema DataSummary { :data-type (s/enum :tags :logentries :events)
                           :status (s/enum :ok :inconsistent)
                           :count s/Num
                           :latest s/Str
                           :oldest s/Str })


(def event-categories (s/enum :consumed :produced :experienced :interacted
                              :formulated :completed :detected :measured :migrated))

; https://www.elastic.co/guide/en/elasticsearch/guide/current/lat-lon-formats.html
(s/defschema Location { :name s/Str
                        ; example, "40.715, -74.011"
                        :coordinates s/Str })

(s/defschema EventObject { :eventId s/Str
                           :created s/Str
                           :occurred s/Str
                           :location (s/maybe Location)
                           :category event-categories
                           :source s/Str
                           :weight s/Num
                           :description s/Str })

(s/defschema Event (merge EventObject { :logEntryLinks [Link] }))

(s/defschema EventRequest { ; Optionally put eventId and created here for migration purpose
                            (s/optional-key :eventId) s/Str
                            (s/optional-key :created) s/Str
                            (s/optional-key :occurred) s/Str
                            (s/optional-key :location) Location
                            :category event-categories
                            :source s/Str
                            :weight s/Num
                            :description s/Str })
