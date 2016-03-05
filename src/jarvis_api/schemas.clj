(ns jarvis-api.schemas
  (:require [schema.core :as s]))


; TODO: Change this "id" field to s/Num
(s/defschema LogEntry { :id s/Str
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

(s/defschema LogEntryPrev { (s/optional-key :id) s/Str
                           :author String
                           :created String
                           (s/optional-key :occurred) String
                           :version String
                           :tags [s/Str]
                           (s/optional-key :parent) (s/maybe s/Str)
                           (s/optional-key :todo) (s/maybe s/Str)
                           (s/optional-key :setting) String
                           :body String })


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

(s/defschema TagPrev { :author s/Str
                      :created s/Str
                      :version s/Str
                      :tags [s/Str]
                      :body s/Str })
