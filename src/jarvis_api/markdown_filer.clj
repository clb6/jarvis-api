(ns jarvis-api.markdown_filer
  (:require [clojure.string :as cs]
            [jarvis-api.config :as config]))


(defn- parse-file-metadata
  "Takes a metadata string, splits each line, then each line is split per
  key-value pair.

  e.g. 'Author: John Doe' becomes { :author 'John Doe' }"
  [metadata]
  (let [[:as metadata-tuples] (map #(cs/split %1 #": ") (cs/split metadata #"\n"))]
    (update-in (reduce (fn [target-map [k v]]
                         (assoc target-map ((comp keyword cs/lower-case) k) v))
                       {}
                       metadata-tuples)
               [:tags] #(cs/split %1 #", "))))

(defn- markdown-to-html-images
  "Takes a body of markdown text and replaces the image syntax with html image
  tags. Returns the new version of the text."
  [body]
  (cs/replace body #"!\[([\-\w]*)\]\(([\w.\-\/]*)\)"
              (format "<img src=\"file://%s/$2\" alt=\"$1\" height=\"750px\" width=\"750px\" />"
                      config/jarvis-images-directory)))

(defn parse-file
  [text]
  (let [[metadata & body] (cs/split text #"\n\n")]
    (assoc (parse-file-metadata metadata) :body
           (markdown-to-html-images (cs/join "\n\n" body)))))

