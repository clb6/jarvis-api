(ns jarvis-api.util)


(defn set-field-default-maybe
  "Set a specified key with a default value if it does not exist already"
  [jarvis-resource metadata-key default]
  (if (not (contains? jarvis-resource metadata-key))
    (assoc jarvis-resource metadata-key default)
    jarvis-resource))
