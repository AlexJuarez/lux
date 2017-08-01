(ns lux.db.fixtures
  (:require
    [korma.core :refer [select limit entity? insert values]]
    [clojure.tools.logging :as log]))

(defn- fixture-path [table]
  (str "resources/fixtures/" table ".edn"))

(defn- get-entity [entity]
  (let [data (select entity (limit 10))]
    {:table (name (:table entity)) :data data}))

(defn- fixture-exists? [entity]
  (-> entity
      :table
      name
      fixture-path
      clojure.java.io/as-file
      .exists))

(defn- get-fixture [{:keys [table] :as entity}]
  (let [{data :data} (clojure.edn/read-string (slurp (fixture-path table)))]
    {:entity entity :data data}))

(defn- contains [item vs]
  (not (= true (some #(= item (name %)) vs))))

(defn- get-entities [blacklist]
  (->> (ns-interns *ns*)
       (map #(var-get (val %)))
       (filter entity?)
       (filter #(contains (:table %) blacklist))
       ))

(defn- get-data [blacklist]
  (->> (get-entities blacklist)
       (map get-entity)
       (filter #(not (empty? (:data %))))))

(defn- get-fixtures []
  (->> (get-entities)
       (filter fixture-exists?)
       (map get-fixture)))

(defn- write-fixture [{:keys [table data]}]
  (with-open [file (clojure.java.io/writer (fixture-path table))]
    (clojure.pprint/pprint {:data data} file)))

(defn- load-fixture [{:keys [entity data]}]
  (insert entity (values data)))

(defn dump! [blacklist]
  (pmap write-fixture (get-data blacklist)))

(defn hydrate! []
  (map load-fixture (get-fixtures)))
