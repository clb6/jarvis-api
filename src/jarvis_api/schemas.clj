(ns jarvis-api.schemas
  (:require [schema.core :as s]))


(s/defschema Link { :href s/Str
                    :rel s/Str
                    (s/optional-key :title) s/Str })


(s/defschema LogEntryObject { :id s/Num
                              :author String
                              :created String
                              :occurred String
                              :version String
                              :tags [s/Str]
                              :parent (s/maybe s/Str)
                              :todo (s/maybe s/Str)
                              :setting String
                              :body String })

(s/defschema LogEntryRequest  { :author String
                                :occurred String
                                :tags [s/Str]
                                (s/optional-key :parent) (s/maybe s/Str)
                                (s/optional-key :todo) (s/maybe s/Str)
                                :setting String
                                :body String })

(s/defschema LogEntry (dissoc (merge LogEntryObject { :tagLinks [Link]
                                                      :parentLink (s/maybe Link) })
                              :tags :parent))


(s/defschema Tag { :name s/Str
                  :author s/Str
                  :created s/Str
                  :version s/Str
                  :tags [s/Str]
                  :body s/Str })

(s/defschema TagRequest { :name s/Str
                         :author s/Str
                         :tags [s/Str]
                         :body s/Str })

(s/defschema TagPrev { (s/optional-key :name) s/Str
                      :author s/Str
                      :created s/Str
                      :version s/Str
                      :tags [s/Str]
                      :body s/Str })


(s/defschema DataSummary { :data-type (s/enum :tags :logentries)
                           :status (s/enum :ok :inconsistent)
                           :count s/Num
                           :latest s/Str
                           :oldest s/Str })
