(ns jarvis-api.database.test.elasticsearch
  (:use [clojure test])
  (:require [jarvis-api.database.elasticsearch :as jes]
            )
  )


(deftest test-adding-query-criterias
  (testing "Add query string"
    (is (= (jes/add-query-criteria-query-string :foo "bar")
           [{:query_string {"query" "foo:*bar*"}}]
           ))
    )
  (testing "Add query match"
    (is (= (jes/add-query-criteria-match :foo "bar")
           [{:match {:foo {:query "bar"}}}]
           ))
    )
  (testing "Add query range"
    (is (= (jes/add-query-criteria-range-gte :foo 100)
           [{:range {:foo {"gte" 100}}}]
           ))
    )
  (testing "General add query"
    ; #' is a reader macro to refer to private functions
    ; http://stackoverflow.com/questions/37471253/how-can-i-write-unit-tests-for-private-clojure-functions
    (let [add-query-criteria #'jes/add-query-criteria]
      (is (= (add-query-criteria hash-map :foo "bar"))
          [{:foo "bar"}])
      (is (= (add-query-criteria hash-map :foo nil))
          [])
      (is (= (add-query-criteria hash-map :ham "sandwich" [{:foo "bar"}]))
          [{:ham "sandwich"} {:foo "bar"}])
      )
    )
  )

