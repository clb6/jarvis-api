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
  [created log-entry-request]
  (let [now-isoformat (tf/unparse (tf/formatters :date-hour-minute-second) created)]
    (assoc log-entry-request :created now-isoformat :version config/jarvis-log-entry-version)))

(defn generate-log-entry-path
  "The file name is  simply the epoch time of the created clj-time/datetime.

  Returns the full path"
  [created]
  (let [log-entry-name (tc/in-seconds (tc/interval (tc/epoch) created))]
    (format "%s/%s.md" config/jarvis-log-directory log-entry-name)))

(defn post-log-entry!
  "Post a new log entry where new entries are appended.

  [log-entry-request :- LogEntryRequest]

  Returns a http-response"
  [log-entry-request]
  (let [created (tc/now)
        log-entry-path (generate-log-entry-path created)
        log-entry-object (create-log-entry-object created log-entry-request)]
    (if (nil? (spit log-entry-path (generate-log-entry-file log-entry-object)))
      (ok))))
