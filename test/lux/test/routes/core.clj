(ns lux.test.routes.core
  (:require [lux.routes :refer :all]
            [clojure.test :refer :all]
            [lux.error :as error]
            [lux.layout :as layout]))

(defmacro bind-error [& body]
  `(binding [error/*errors* (atom {})]
     ~@body))

(defpage test-page
  :template ["test.html" {:a "present"}])

(defpage test-page-body
  :template ["test.html" {:a "present"}]
  (fn [slug] {:body "test"}))

(deftest defpage-test
  (with-redefs [layout/render (fn [p & args] {:template p :args (apply merge args)})]
    (bind-error
      (is (= (get-in (test-page {:slug "slug"}) [:args :slug])))
      (is (= (-> (test-page) :template) "test.html"))
      (is (= (-> (test-page) :args :a) "present"))
      (is (= (-> (test-page-body {}) :body) "test"))
      )))




