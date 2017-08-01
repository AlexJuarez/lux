(ns lux.routes
  (:require
    [lux.schema]
    [compojure.api.sweet :refer :all]
    [compojure.api.common :refer [extract-parameters]]
    [lux.error :as error]))

(defmacro defpage [page-name & body]
  (let [[params form] (extract-parameters body true)
        [template & args] (get params :template)
        success (get params :success)]
    `(defn ~page-name
       ([]
        (layout/render ~template ~@args))
       ([slug#]
        (if (error/empty?)
          (do
            (~@form slug#)
            (when-not (nil? ~success)
              (layout/render ~template slug# {:message {:body ~success :type :success}})))
          (layout/render ~template slug# ~@args))))))

(defn page-route [route page Schema]
  (context
    route []
    (GET "/" []
         (page))
    (POST "/" []
          :form [info Schema]
          (page info))))
