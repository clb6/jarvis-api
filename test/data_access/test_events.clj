(ns data-access.test-events
  (:use [clojure test])
  (:require [jarvis-api.data_access.events :as jde]
            )
  )


(def event-object { :eventId "event-abc" :created "2016-12-31T02:00"
                    :occurred "2016-12-31T03:00" :category :interacted
                    :source "unit-test" :weight 1000 :location nil
                    :description "I am doing a unit test" })

(def event-rollback { :eventId "event-xyz" :created "2016-11-31T02:00"
                      :occurred "2016-11-31T03:00" :category :interacted
                      :source "unit-test" :weight 100 :location nil
                      :description "This is the fake previous event"
                      :logEntries ["234" "567"]
                      :artifacts [ { :href "http://flatland.info/sphere"
                                     :rel "some-other-note" } ] })


(deftest test-make-event-from-object
  (let [log-ids ["123" "456" "789"]
        artifacts [ { :href "http://sample.com/yolo" :rel "some-sample" } ]
        expected-event (assoc event-object :logEntries log-ids :artifacts artifacts)]
    (letfn [(get-log-entry-ids [event-id]
              log-ids)
            (get-artifacts [event-id]
              artifacts)
            ]
      (is (= (jde/make-event-from-object get-log-entry-ids get-artifacts event-object)
             expected-event))
      (is (= (jde/make-event-from-object get-log-entry-ids get-artifacts nil)
             nil))
      ))
  )

(deftest test-write-event
  (let [log-ids ["123" "456" "789"]
        artifacts [ { :href "http://sample.com/yolo" :rel "some-sample" } ]
        expected-event (assoc event-object :logEntries log-ids :artifacts artifacts)]

    (letfn [(make-event [event-object]
              (assoc event-object :logEntries log-ids :artifacts artifacts))]

      (testing "Success scenario"
        (letfn [(put-event-object [event-id event-object]
                  event-object)
                (update-artifacts [event-id artifacts]
                  (count artifacts))
                ]
          (is (= (jde/write-event put-event-object update-artifacts make-event
                                  event-object artifacts)
                 expected-event))))

      (testing "Failure scenarios with no rollback"
        (letfn [(put-event-object [event-id event-object]
                  nil)
                (update-artifacts [event-id artifacts]
                  (count artifacts))
                ]
          (is (= (jde/write-event put-event-object update-artifacts make-event
                                  event-object artifacts)
                 nil)))
        (letfn [(put-event-object [event-id event-object]
                  (throw (RuntimeException. "Fake out error")))
                (update-artifacts [event-id artifacts]
                  (count artifacts))
                ]
          (is (= (jde/write-event put-event-object update-artifacts make-event
                                  event-object artifacts)
                 nil)))
        (letfn [(put-event-object [event-id event-object]
                  event-object)
                (update-artifacts [event-id artifacts]
                  0)
                ]
          (is (= (jde/write-event put-event-object update-artifacts make-event
                                  event-object artifacts)
                 nil)))
        (letfn [(put-event-object [event-id event-object]
                  event-object)
                (update-artifacts [event-id artifacts]
                  (throw (RuntimeException. "Fake out error")))
                ]
          (is (= (jde/write-event put-event-object update-artifacts make-event
                                  event-object artifacts)
                 nil)))
        )

      (testing "Failure scenarios with rollback"
        (letfn [(put-event-object [event-id event-object]
                  (if (= event-id (:eventId event-rollback))
                    (dissoc event-rollback :logEntries :artifacts)
                    nil))
                (update-artifacts [event-id artifacts]
                  (count artifacts))
                ]
          (is (= (jde/write-event put-event-object update-artifacts make-event
                                  event-object artifacts event-rollback)
                 nil)))
        (letfn [(put-event-object [event-id event-object]
                  (if (= event-id (:eventId event-rollback))
                    (dissoc event-rollback :logEntries :artifacts)
                    (throw (RuntimeException. "Fake out error"))
                    ))
                (update-artifacts [event-id artifacts]
                  (count artifacts))
                ]
          (is (= (jde/write-event put-event-object update-artifacts make-event
                                  event-object artifacts event-rollback)
                 nil)))
        )

      (testing "Failure scenarios with failed rollback"
        (letfn [(put-event-object [event-id event-object]
                  (throw (RuntimeException. "Fake out error - database out")))
                (update-artifacts [event-id artifacts]
                  (count artifacts))
                ]
          (is (= (jde/write-event put-event-object update-artifacts make-event
                                  event-object artifacts event-rollback)
                 nil)))
        )

    ))
  )
