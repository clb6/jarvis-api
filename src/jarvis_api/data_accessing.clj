(ns jarvis-api.data-accessing
  (:require [schema.core :as s]
            [taoensso.timbre :as timbre :refer [warn error]]
            ))

; Collection of pure, closure functions that is to be used for transactional
; operations


(defn create-retrieve-func
  "Create function to retrieve a resource object

  High order function that creates a function that takes a sequence of get
  functions that are threaded together to form and to return a resource object.
  The retrieve will short-circuit and return nil if any of the gets return nil.

  Args:
    get-funcs: Sequence of fn[resource-id resource-object] -> maybe resource-object

  Returns:
    fn[resource-id resource-object] -> maybe resource-object
  "
  [& get-funcs]
  (fn [resource-id resource-object]
    (letfn [(wrap-get-func [get-func]
              (fn [resource-id resource-object]
                (if resource-object
                  (get-func resource-id resource-object)))
              )]
      (let [wrapped-get-funcs (map wrap-get-func get-funcs)
            wrapped-get-funcs (map #(partial % resource-id) wrapped-get-funcs)]
        ((apply comp wrapped-get-funcs) resource-object)
        )))
  )


(defn create-write-func
  "Creates function to write a resource object

  High order function that creates a function that tries to write a resource
  object by calling a sequence of put functions.  If any of the put functions
  returns nil then the whole transaction short circuits and nil is returned.

  Args:
    put-funcs: Sequence of fn[resource-id resource-object] -> maybe resource-object

  Returns:
    fn[resource-id resource-object] -> maybe resource-object"
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
  "Creates function to remove a resource object

  High order function that creates a function that tries to remove a resource
  object by calling a sequence of delete functions.  All the delete functions are
  attempted and there is no short circuiting.  If all the deletes return true
  then the entire remove call returns true, else false.

  Args:
    put-funcs: Sequence of fn[resource-id resource-object] -> boolean

  Returns:
    fn[resource-id resource-object] -> boolean"
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
  resource object if it exists else deletes the currently existing one.

  Args:
    write-func: fn[resource-id resource-object] -> maybe resource-object
    remove-func: fn[resource-id resource-object] -> boolean

  Returns:
    fn[resource-id resource-object] -> boolean"
  ; TODO: Fix? Have a more consistent retrun?
  [write-func remove-func]
  (fn [resource-id resource-object-prev]
    (try
      (if resource-object-prev
        ; Rolling back via restoring previous version
        (let [resource-object (write-func resource-id resource-object-prev)]
          (if resource-object
            (do
              (warn "Roll back via restoring: ok, " resource-id)
              true)
            (do
              (error "Roll back via restoring: failed, " resource-id)
              false)
            ))
        ; Rolling back via cleaning up the current version
        (let [result (remove-func resource-id resource-object-prev)]
          (if result
            (warn "Roll back via clean up: ok, " resource-id)
            (error "Roll back via clean up: failed, " resource-id))
          result
          )
        )
      (catch Exception e
        (error "Unexpected rollback failure")
        false)
      ))
  )


(defn create-write-reliably-func
  "Creates function to write a resource object reliably

  Reliably means that if something bad happens while trying to write, then an
  attempt to rollback is made.  Returns only the successfully written resource
  object else nil.

  Args:
    get-func: fn[resource-id] -> resource object, used to retrieve the previous resource
      object if it exists
    write-func: fn[resource-id resource-object] -> maybe resource object
    rollback-func: fn[resource-id resource-object] -> boolean
    get-resource-id-func: fn[resource-object] -> any

  Returns:
    fn[resource-object] -> maybe resource-object
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
