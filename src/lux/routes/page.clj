(ns lux.routes
  (:require
    [lux.schema]
    [compojure.api.sweet :refer :all]
    [compojure.api.common :refer [extract-parameters]]
    [lux.layout :as layout]
    [lux.message :as message]
    [clojure.tools.logging :as log]
    [lux.error :as error]))

(defn page-route [route page Schema]
  (context
    route []
    (GET "/" []
         (page))
    (POST "/" []
          :form [info Schema]
          (page info))))

(defn resolvefn [obj args]
  (if (vector? obj)
    (let [[k v] obj]
      (if (fn? v)
        [k (v args)]
        [k v]))
    obj))

(defn apply-fns [lst args]
  (clojure.walk/prewalk
    #(resolvefn % args)
    lst))

(defn update-template [args body]
  (map #(apply-fns % args) body))

(defn prune [obj]
  (if (and (vector? obj) (= (count obj) 2))
    (let [[k v] obj]
      (prn k v)
      (if (nil? v)
        nil
        obj))
    obj))

(defn- render-page [obj opts]
  (if-let [template (get opts :template)]
    (assoc-in
      obj [:fns :render]
      (fn [& params] `(layout/render ~template ~@params)))
    obj))

(defn- page-success [obj opts]
  (if-let [success (get opts :success)]
    (assoc-in
      obj [:fns :success]
      (fn [] `(message/success! ~success)))
    obj))

(defn- page-validator [obj opts]
  (if-let [validator (get opts :validator)]
    (assoc-in
      obj [:fns :validator]
      (fn [& args] `(validator ~@args)))
    obj))

(defn- parse-options [page-name & body]
  (let [[params form] (extract-parameters body true)
        [template & template-body] (get params :template)]
    (clojure.walk/prewalk
       prune
       (-> {:page-name page-name
            :template-path template
            :template-body template-body
            :body (apply list form)}
           (render-page params)
           (page-success params)
           (page-validator params)
           ))))

(defn- resolve-page [options]
  )

(defmacro defpage [page-name & body]
  `(let [options# (parse-options '~page-name ~@body)]
     options#))

(defpage test-page
  :template ["test.html"]
  :validator (fn [slug] (str "hello " slug)))
