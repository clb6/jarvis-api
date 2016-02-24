(ns jarvis-api.resources.logentries
  (:require [clojure.string :as cs]
            [clj-time.core :as tc]
            [clj-time.format :as tf]
            [ring.util.http-response :refer :all]
            [jarvis-api.config :as config]
            [jarvis-api.markdown_filer :as mf]))


(defn get-log-entry!
  "Return web response where if ok, returns a log entry object"
  [id]
  (let [log-entry-path (format "%s/%s.md" config/jarvis-log-directory id)]
    (if (.exists (clojure.java.io/as-file log-entry-path))
      (ok (mf/parse-file (slurp log-entry-path)))
      (not-found))))


(defn generate-file-metadata
  [metadata-keys log-entry-object]
  (letfn [(get-metadata-value [mk]
            (let [value (get log-entry-object mk)]
              (if (= clojure.lang.PersistentVector (type value))
                (cs/join ", " value)
                value)))
          (generate-line [mk]
            (cs/join ": " (list (cs/capitalize (name mk)) (get-metadata-value mk))))]
    (cs/join "\n" (map generate-line metadata-keys))))

(defn generate-file
  [metadata-keys log-entry-object]
  (cs/join "\n\n" (list (generate-file-metadata metadata-keys log-entry-object)
                        (get log-entry-object :body))))

; Tried to use the `keys` method from the schema but the sort order is not
; predictable. Maybe use two separate vectors to construct the schema via zip.
(def metadata-keys-log-entries (list :author :created :occurred :version :tags
                                     :parent :todo :setting))
(def generate-log-entry-file (partial generate-file metadata-keys-log-entries))

(defn create-log-entry-object
  "TODO: Reinforce Schema. I tried this:

  [log-entry-request :- jas/LogEntryRequest]

  but I got this error:

  CompilerException java.lang.RuntimeException: Can't use qualified name as parameter: jas/LogEntryRequest, compiling:(jarvis_api/resources/logentries.clj:38:1)"
  [log-entry-request]
  (let [now-isoformat (tf/unparse (tf/formatters :date-hour-minute-second) (tc/now))]
    (assoc log-entry-request :created now-isoformat :version config/jarvis-log-entry-version)))

(defn post-log-entry!
  [log-entry-request]
  ; Create file name
  ; Create object
  ; Generate log entry file
  )
