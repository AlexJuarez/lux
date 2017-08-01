(ns lux.test.mem-store
  (:require [lux.mem-store :as store]
            [clojure.test :refer :all]))

(defmacro bind-store [& assertions]
  `(binding [store/mem (atom {})]
    ~@assertions))

(deftest test-mem-store
  (testing "set"
    (bind-store
      (store/set :test 1)
      (is (= 1 (store/get :test)))))

  (testing "get"
    (bind-store
      (is (nil? (store/get :test)))))

  (testing "cache!"
    (bind-store
      (store/set :cnt 0)
      (let [cache-fn
            (fn []
              (store/cache!
                :test (let [cnt (inc (store/get :cnt))]
                         (store/set :cnt cnt)
                         cnt)))]
        (is
          (= (cache-fn)
             (store/get :test)))
        (is
          (= (cache-fn)
             (store/get :cnt)))))))
