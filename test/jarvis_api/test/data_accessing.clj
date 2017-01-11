(ns jarvis-api.test.data-accessing
  (:use [clojure test])
  (:require [jarvis-api.data-accessing :as jda]
            )
  )

; UPON DELETE
; {:found true, :_index "jarvis-20160628", :_type "tags", :_id "first", :_version 3, :_shards {:total 2, :successful 1, :failed 0}}

(deftest test-create-rollback-func
  (letfn [(put-func [resource-id resource-object]
            resource-object)
          (delete-func [resource-id]
            nil)]
    (let [rollback-func (jda/create-rollback-func put-func delete-func)
          resource-object-prev { :resourceId "abc" :someValue 123 }
          resource-id (:resourceId resource-object-prev)]
      (is (= (rollback-func resource-id resource-object-prev) resource-object-prev))
      )
    )
  )
