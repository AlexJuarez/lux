(ns lux.routes
  (:require
    [lux.schema]
    [compojure.api.sweet :refer :all]
    [compojure.api.common :refer [extract-parameters]]
    [lux.layout :as layout]
    [lux.message :as message]
    [clojure.tools.logging :as log]
    [lux.error :as error]))

(defn page-route [route page Schema & args]
  (context
    route []
    (GET "/" []
         (apply page args))
    (POST "/" []
          :form [info Schema]
          (apply page info args))))

(defn resolvefn [obj args]
  (if (vector? obj)
    (let [[k v] obj]
      (if (fn? v)
        [k (apply v args)]
        [k v]))
    obj))

(defn apply-fns [lst args]
  (clojure.walk/prewalk
    #(resolvefn % args)
    lst))

(defn update-template [args body]
  (->>
    body
    (map
      #(if (fn? %)
         (apply % args)
         (apply-fns % args)))
    (apply merge)))

(defn- render-page [obj]
  (let [template-path (get obj :template-path)
        render (get obj :render)]
    (fn [& params] (apply render template-path params))))

(defn- page-success [obj]
  (let [success (get obj :success)]
    (fn [] (when success (message/success! success)))))

(defn- page-validator [obj]
  (let [validator (get obj :validator (fn [_]))]
    (fn [& args] (apply validator args))))

(defn- template-params [obj]
  (let [template-body (get obj :template-body ())]
    (fn [& args] (update-template args template-body))))

(defn parse-options [body]
  (let [[params form] (extract-parameters body true)
        [template & template-body] (get params :template)]
    (->
      params
      (dissoc :template)
      (assoc
        :template-path template
        :template-body template-body
        :body (or (last form) (fn [& _] ))
        ))))

(defn resolve-page [fargs & body]
  (let [options (parse-options body)
        render (render-page options)
        params (template-params options)
        validator (page-validator options)
        success (page-success options)
        {:keys [args body]} options]
    (if
      (= (inc (count args)) (count fargs))
      (let [[slug & r] fargs]
        (validator slug)
        (if (error/empty?)
          (let [result (apply body fargs)]
            (success)
            (log/debug result)
            (if (and result (:body result))
              result
              (render (apply params r) slug)))
          (render (apply params r) slug)))
      (render (apply params fargs)))))

(defmacro defpage [page-name & body]
  `(def
     ~page-name
     (fn [& args#]
       (resolve-page args# ~@body))))
