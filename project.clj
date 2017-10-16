(defproject lux "0.1.1-beta"

  :description "A collection of tools to make life easier"
  :dependencies [
                  [korma "0.4.2"] ;;sql dsl
                  [clojurewerkz/scrypt "1.2.0"]
                  [compojure "1.6.0"]
                  [clojurewerkz/spyglass "1.1.0"] ;; couchbase interface
                  [org.clojure/core.match "0.3.0-alpha4"]
                  [prismatic/schema "1.1.6"]
                  [metosin/compojure-api "2.0.0-alpha1"]
                  [org.clojure/clojure "1.8.0"]
                  [org.clojure/tools.logging "0.4.0"]
                  [selmer "1.10.7"] ;; templating
                  [ring/ring-anti-forgery "1.1.0"]
                  [metosin/ring-http-response "0.9.0"]
                  [buddy "1.1.0"] ;; authenication rules
                  [markdown-clj "0.9.99"]
                ]
  :plugins [[lein-drip "0.1.1-SNAPSHOT"]]
  :profiles {:dev {:plugins [[com.jakemccrary/lein-test-refresh "0.21.1"]]}}
  :min-lein-version "2.0.0"
  :source-paths ["src"]
  :test-paths ["test"]
  :target-path "target/%s/")
