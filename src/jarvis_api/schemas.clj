(ns jarvis-api.schemas
  (:require [schema.core :as s]))


(s/defschema Link { :href s/Str
                    :rel s/Str
                    (s/optional-key :title) s/Str })


(s/defschema LogEntryObject { :id s/Num
                              :author String
                              :created String
                              :modified s/Str
                              :occurred String
                              :version String
                              :tags [s/Str]
                              :parent (s/maybe s/Str)
                              :event (s/maybe s/Str)
                              :todo (s/maybe s/Str)
                              :setting String
                              :body String })

(s/defschema LogEntryRequest  { (s/optional-key :id) s/Num
                                :author String
                                ; Optionally put created here for migration purpose
                                (s/optional-key :created) s/Str
                                :occurred String
                                :tags [s/Str]
                                (s/optional-key :parent) (s/maybe s/Str)
                                (s/optional-key :event) (s/maybe s/Str)
                                (s/optional-key :todo) (s/maybe s/Str)
                                :setting String
                                :body String })

(s/defschema LogEntry (dissoc (merge LogEntryObject { :tagLinks [Link]
                                                      :parentLink (s/maybe Link) })
                              :tags :parent))


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


(s/defschema DataSummary { :data-type (s/enum :tags :logentries)
                           :status (s/enum :ok :inconsistent)
                           :count s/Num
                           :latest s/Str
                           :oldest s/Str })
