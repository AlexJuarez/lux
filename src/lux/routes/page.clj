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
        [k (apply v args)]
        [k v]))
    obj))

(defn apply-fns [lst args]
  (clojure.walk/prewalk
    #(resolvefn % args)
    lst))

(defn update-template [args body]
  (into {} (apply-fns body args)))

(defn prune [obj]
  (if (and (vector? obj) (= (count obj) 2))
    (let [[k v] obj]
      (when-not (nil? v) obj))
    obj))

(defn- render-page [obj _]
  (let [template-path (get obj :template-path)]
    (assoc-in
      obj [:fns :render]
      (fn [& params] (layout/render template-path params)))))

(defn- page-success [obj opts]
  (let [success (get opts :success)]
    (assoc-in
      obj [:fns :success]
      (fn [] (when success (message/success! success))))))

(defn- page-validator [obj opts]
  (let [validator (get opts :validator (fn [_]))]
    (assoc-in
      obj [:fns :validator]
      (fn [& args] (apply validator args)))))

(defn- template-params [obj _]
  (let [template-body (get obj :template-body ())]
    (assoc-in
      obj [:fns :params]
      (fn [& args] (update-template args template-body)))))

(defn parse-options [& body]
  (let [[params form] (extract-parameters body true)
        [template & template-body] (get params :template)
        args (get params :args)]
    (clojure.walk/prewalk
       prune
       (-> {:template-path template
            :template-body (apply merge (flatten template-body))
            :args args
            :body (apply list form)}
           (render-page params)
           (page-success params)
           (page-validator params)
           (template-params params)
           ))))

(defn resolve-page [& body]
  (let [options (apply parse-options body)]
    (let [render (get-in options [:fns :render])
          params (get-in options [:fns :params])
          validator (get-in options [:fns :validator])
          success (get-in options [:fns :success])
          {:keys [args body]} options]
      (fn [& fargs]
        (if
          (= (inc (count args)) (count fargs))
          (let [[slug & r] fargs]
            (validator slug)
            (if (error/empty?)
              (let [results (map #(% slug) body)
                    result (last results)]
                (success)
                (log/debug result)
                (if (:body result)
                  result
                  (render slug (params r))))
              (render slug (params r))))
          (render (params fargs)))
        ))))

(defmacro defpage [page-name & body]
  ^{:doc
    "Creates a page function, the macro expects page-name, key value options followed by a body
    to execute on success.
    (defpage test-page
    :template [\"test-page.html\" {:hello \"world\"}]
    :validator (fn [slug] ... )
    :success \"Success message to display\")

    ### options:

    - **:template**                 Define a template-path & args.
    - **:validator**                Define a custom slug validator.
    - **:success**                  Define a message to show on success.
    "
    }
  `(def
     ~page-name
     (resolve-page ~@body)
     ))
