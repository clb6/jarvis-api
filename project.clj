(defproject jarvis-api "0.1.0"
  :description "Data API for Jarvis"
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [clj-time "0.11.0"] ; required due to bug in lein-ring
                 [metosin/compojure-api "0.22.0"]
                 [ring.middleware.logger "0.5.0"]
                 [clj-logging-config "1.9.12"]]
  :ring {:init jarvis-api.handler/init-app
         :handler jarvis-api.handler/app-with-logging}
  :uberjar-name "server.jar"
  :profiles {:dev {:dependencies [[javax.servlet/servlet-api "2.5"]]
                   :plugins [[lein-ring "0.9.6"]]}})
