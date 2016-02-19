(ns jarvis-api.resources.logentries
  (:require [jarvis-api.config :as config]
            [jarvis-api.markdown_filer :as mf]))


(defn get-log-entry!
  [id]
  (let [log-entry-path (format "%s/%s.md" config/jarvis-log-directory id)
        log-entry (slurp log-entry-path)]
    (mf/parse-file log-entry)))
