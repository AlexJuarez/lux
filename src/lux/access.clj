(ns lux.access
  (:require
    [buddy.auth.accessrules :refer [restrict]]))

(defn wrap-restricted [handler rule]
  (restrict handler {:handler  (:handler rule)
                     :on-error (:on-error rule)}))
