(defproject jarvis-api "0.9.0"
  :description "Data API for Jarvis"
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [clj-time "0.11.0"] ; required due to bug in lein-ring
                 [metosin/compojure-api "1.0.2"]
                 [ring.middleware.logger "0.5.0"]
                 [clj-logging-config "1.9.12"]
                 [clojurewerkz/elastisch "2.2.1"]
                 [org.clojure/tools.logging "0.3.1"]
                 ; FYI: Looked into using clojurewerkz/urly but saw a comment from
                 ; the maintainer that its no longer being maintained and
                 ; recommended exploding-fish
                 [org.bovinegenius/exploding-fish "0.3.4"]]
  :ring {:init jarvis-api.handler/init-app
         :handler jarvis-api.handler/app-with-logging}
  :uberjar-name "server.jar"
  :profiles {:dev {:dependencies [[javax.servlet/servlet-api "2.5"]]
                   :plugins [[lein-ring "0.9.6"]]}})
