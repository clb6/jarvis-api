(ns jarvis-api.test.data-accessing
  (:use [clojure test])
  (:require [jarvis-api.data-accessing :as jda]
            ))


(deftest test-retrieve-func
  (letfn [(get-func-good-one [id object]
            (assoc object :one "red"))
          (get-func-good-two [id object]
            (assoc object :two "blue"))
          (get-func-bad-one [id object]
            nil)]
    (testing "Good case"
      (let [retrieve-func (jda/create-retrieve-func get-func-good-one
                                                    get-func-good-two)]
        (is (= (retrieve-func "some-id" {}) { :one "red" :two "blue" }))))
    ; Bad case - sandwiched
    (testing "Bad case: quiet failure"
      (let [retrieve-func (jda/create-retrieve-func get-func-good-one
                                                    get-func-bad-one
                                                    get-func-good-two)]
        (is (= (retrieve-func "some-id" {}) nil))))
    ))


(deftest test-write-func
  (let [resource-object { :id "abc" :something "some-value" }]
    (letfn [(put-func-good [id object]
              object)
            (put-func-bad [id object]
              nil)
            (put-func-exception [id object]
              (throw (Exception. "Yup failed write")))]
      (testing "Good case"
        (let [retrieve-func (jda/create-write-func put-func-good)]
          (is (= (retrieve-func (:id resource-object) resource-object)
                 resource-object))))
      (testing "Bad case: quiet failure"
        (let [retrieve-func (jda/create-write-func put-func-good put-func-bad)]
          (is (= (retrieve-func (:id resource-object) resource-object) nil))))
      (testing "Bad case: exception"
        (let [retrieve-func (jda/create-write-func put-func-good put-func-exception)]
          (is (= (retrieve-func (:id resource-object) resource-object) nil))))
        )))


(deftest test-remove-func
  (let [resource-object { :id "abc" :something "some-value" }]
    (letfn [(delete-func-good [id object]
              true)
            (delete-func-bad [id object]
              false)
            (delete-func-nil [id object]
              nil)
            (delete-func-exception [id object]
              (throw (Exception. "Yup failed delete")))]
      (testing "Good case"
        (let [remove-func (jda/create-remove-func delete-func-good
                                                  delete-func-good
                                                  delete-func-good)]
          (is (= (remove-func (:id resource-object) resource-object) true))))
      (testing "Bad case: quiet failure"
        (let [remove-func (jda/create-remove-func delete-func-good
                                                  delete-func-bad)]
          (is (= (remove-func (:id resource-object) resource-object) false))))
      (testing "Bad case: exception failure"
        (let [remove-func (jda/create-remove-func delete-func-good
                                                  delete-func-exception)]
          (is (= (remove-func (:id resource-object) resource-object) false))))
      )))


(deftest test-rollback-func
  (let [resource-object { :id "abc" :something "some-value" }]
    (letfn [(write-func-good [id object]
              resource-object)
            (write-func-bad [id object]
              nil)
            (write-func-never [id object]
              (throw (Exception. "Why was this write func called?")))
            (remove-func-good [id object]
              (if id
                true
                false))
            (remove-func-bad [id object]
              false)
            (remove-func-never [id object]
              (throw (Exception. "Why was this remove func called?")))]
      (testing "Good case: without previous version"
        (let [rollback-func (jda/create-rollback-func write-func-never
                                                      remove-func-good)]
          (is (= (rollback-func (:id resource-object) nil) true))))
      (testing "Good case: with previous version"
        (let [rollback-func (jda/create-rollback-func write-func-good
                                                      remove-func-never)]
          (is (= (rollback-func (:id resource-object) resource-object) true))))
      (testing "Bad case: without previous version, quiet failure"
        (let [rollback-func (jda/create-rollback-func write-func-never
                                                      remove-func-bad)]
          (is (= (rollback-func (:id resource-object) nil) false))))
      (testing "Bad case: with previous version, quiet failure"
        (let [rollback-func (jda/create-rollback-func write-func-bad
                                                      remove-func-never)]
          (is (= (rollback-func (:id resource-object) resource-object) false))))
      (testing "Bad case: without previous version, exception failure"
        (let [rollback-func (jda/create-rollback-func write-func-good
                                                      remove-func-never)]
          (is (= (rollback-func (:id resource-object) nil) false))))
      (testing "Bad case: with previous version, exception failure"
        (let [rollback-func (jda/create-rollback-func write-func-never
                                                      remove-func-good)]
          (is (= (rollback-func (:id resource-object) resource-object) false))))
      )))


(deftest test-write-reliably-func
  (let [resource-object { :id "abc" :something "some-value" }]
    (letfn [(get-func-good [id]
              nil)
            (get-id-func [object]
              (:id object))
            (write-func-good [id object]
              object)
            (write-func-bad [id object]
              nil)
            (rollback-func-good [id object]
              true)
            (rollback-func-bad [id object]
              false)
            (rollback-func-never [id object]
              (throw (Exception. "Why was this rollback func called?")))]
      (testing "Good case: "
        (let [write-reliably-func (jda/create-write-reliably-func get-func-good
                                                                  write-func-good
                                                                  rollback-func-never
                                                                  get-id-func)]
          (is (= (write-reliably-func resource-object) resource-object))))
      (testing "Badish case: write failed, rollback ok"
        (let [write-reliably-func (jda/create-write-reliably-func get-func-good
                                                                  write-func-bad
                                                                  rollback-func-good
                                                                  get-id-func)]
          (is (= (write-reliably-func resource-object) nil))))
      (testing "Bad case: write failed, rollback failed"
        (let [write-reliably-func (jda/create-write-reliably-func get-func-good
                                                                  write-func-bad
                                                                  rollback-func-bad
                                                                  get-id-func)]
          (is (= (write-reliably-func resource-object) nil))))
      )))

