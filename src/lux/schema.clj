(ns lux.schema
  (:require
    [clojure.walk :as walk]
    [compojure.api.middleware :as mw]
    [compojure.api.meta :as m :refer [restructure-param]]
    [compojure.api.coerce :refer [cached-coercer]]
    [lux.error :as error]
    [lux.session :as session]
    [lux.access :refer [wrap-restricted]]
    [schema.core :as s]
    [schema.utils :as su]))

(defn coerce! [schema key type request]
  (let [value (walk/keywordize-keys (key request))]
    (if-let [matchers (mw/coercion-matchers request)]
      (if-let [matcher (matchers type)]
        (let [coercer (cached-coercer request)
              coerce (coercer schema matcher)
              result (coerce value)]
          (if (su/error? result)
            (do
              (error/set-from-validation! result)
              value)
            result))
        value)
      value)))

(defn src-coerce!
  [schema key type]
  `(coerce! ~schema ~key ~type ~m/+compojure-api-request+))

(defmethod restructure-param :access-rule
  [_ rule acc]
  (update-in acc [:middleware] conj [wrap-restricted rule]))

(defmethod restructure-param :form
  [_ [value schema] acc]
  (-> acc
      (update-in [:letks] into [value (src-coerce! schema :form-params :string)])
      (assoc-in [:swagger :parameters :formData] schema)
      (assoc-in [:swagger :consumes] ["application/x-www-form-urlencoded"])))

(defmethod restructure-param :multipart-form
  [_ [value schema] acc]
  (-> acc
      (update-in [:lets] into [value (src-coerce! schema :multipart-params :string)])
      (assoc-in [:swagger :parameters :formData] schema)
      (assoc-in [:swagger :consumes] ["multipart/form-data"])))
