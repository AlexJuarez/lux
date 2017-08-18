(ns lux.test.routes.core
  (:require [lux.routes :refer :all]
            [clojure.test :refer :all]
            [lux.error :as error]
            [lux.layout :as layout]))

(defmacro bind-error [& body]
  `(binding [error/*errors* (atom {})]
     ~@body))

(defpage test-template-body
  :template ["test.html" {:a "present" :function (fn [& args] (str "world")) :eval (str "hello")}])

(defpage test-page-results
  :template ["test.html" {:a "present"}]
  (fn [slug] {:body "test"}))

(defpage test-validator-page
  :template ["test.html" {:test "test"}]
  :validator (fn [slug] (error/register! :slug slug)))

(deftest defpage-test
  (with-redefs [layout/render (fn [p & args] {:template p :args (apply merge (flatten args))})]
    (testing "defpage macro"
      (testing "template body"
        (bind-error
          (is (= (get-in (test-template-body {:slug "slug"}) [:args :slug])))
          (is (= (-> (test-template-body) :template) "test.html"))
          (is (= (-> (test-template-body) :args :function) "world"))
          (is (= (-> (test-template-body) :args :eval) (str "hello")))))
      (testing "results"
        (bind-error
          (is (= (-> (test-page-results {}) :body) "test"))))
      (testing "validator"
        (bind-error
          (is (= (-> (test-validator-page {:a 1}) :args :a) 1))
          (is (= (first (error/get :slug)) {:a 1}) ))))))

