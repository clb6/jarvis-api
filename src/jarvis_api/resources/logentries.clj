(ns jarvis-api.resources.logentries
  (:require [ring.util.http-response :refer :all]
            [jarvis-api.config :as config]
            [jarvis-api.markdown_filer :as mf]))


(defn get-log-entry!
  "Return web response where if ok, returns a log entry object"
  [id]
  (let [log-entry-path (format "%s/%s.md" config/jarvis-log-directory id)]
    (if (.exists (clojure.java.io/as-file log-entry-path))
      (ok (mf/parse-file (slurp log-entry-path)))
      (not-found))))
