(ns jarvis-api.data-accessing
  (:require [schema.core :as s]
            [taoensso.timbre :as timbre :refer [warn error]]
            ))

; prev exists:
; put fails..nothing, nothing
; add fails..put prev, nothing
; remove fails..put prev, nothing
;
; first:
; put fails..nothing, nothing
; add fails..delete, nothing
; remove fials..delete, nothing


(defn create-write-func
  "Creates function to write resource object

  High order function that creates a function that tries to write resource
  object.

  Args:
    put-funcs: Arbitrary list of fn[resource-id resource-object]

  Returns:
    fn[resource-id resource-object] that returns nil or resource-object"
  [& put-funcs]
  (fn [resource-id resource-object]
    (letfn [(wrap-put-func [put-func]
              (fn [resource-id resource-object]
                (if resource-object
                  (put-func resource-id resource-object)))
              )]
      (try
        (let [wrapped-put-funcs (map wrap-put-func put-funcs)
              wrapped-put-funcs (map #(partial % resource-id) wrapped-put-funcs)
              result ((apply comp wrapped-put-funcs) resource-object)
              ]
          (or result (error "Failed to write resource object"))
          )
        (catch Exception e
          (error "Unexpected exception: " (.getMessage e)))
        )))
  )

(defn create-remove-func
  "Creates function to write resource object

  High order function that creates a function that tries to write resource
  object.

  Args:
    put-funcs: Arbitrary list of fn[resource-id resource-object]

  Returns:
    fn[resource-id resource-object] that returns nil or resource-object"
  [& delete-funcs]
  (fn [resource-id resource-object]
    (try
      (let [result (map #(% resource-id resource-object) delete-funcs)]
        (if (every? true? result)
          true
          (do
            (error "Failed to delete resource object")
            false)
          ))
      (catch Exception e
        (do
          (error "Unexpected exception: " (.getMessage e))
          false))
      ))
  )


(defn create-rollback-func
  "Creates rollback function

  High order function that creates a function that tries to put the previous
  resource object or deletes the existing one.

  Args:
    put-func: fn[resource-id resource-object]
    delete-func: fn[resource-id]

  Returns:
    fn[resource-id resource-object] that returns nil or true"
  [write-func remove-func]
  (fn [resource-id resource-object-prev]
    (try
      (if resource-object-prev
        (write-func resource-id resource-object-prev)
        (remove-func resource-id resource-object-prev)
        )
      (catch Exception e
        (error "Unexpected rollback failure"))
      ))
  )


(defn create-write-reliably-func
  "Creates function to writes resource object reliably

  Reliably means that if something bad happens while trying to write, then will
  try to rollback.

  Args:
    get-func: fn[resource-id]
    write-func: fn[resource-id resource-object]
    rollback-func: fn[resource-id resource-object]
    get-resource-id-func: fn[resource-object]

  Returns:
    fn[resource-object] that returns nil or resource-object
  "
  [get-func write-func rollback-func get-resource-id-func]
  (fn [resource-object]
    ; Pass get-func that takes resource-object?
    (let [resource-id (get-resource-id-func resource-object)
          resource-object-prev (get-func resource-id)
          resource-object (write-func resource-id resource-object)]
      (if (nil? resource-object)
        (do
          (rollback-func resource-id resource-object-prev)
          ; REVIEW: Can I do better than just return nil?
          nil)
        resource-object)))
  )
