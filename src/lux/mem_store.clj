(ns lux.mem-store
  (:require [lux.routes :as routes])
  (:refer-clojure :exclude [get set]))

(def ^:dynamic mem (atom {}))

(defn now [ttl]
  (+ (/ (System/currentTimeMillis) 1000) ttl))

(defn delete [key]
  (swap! mem dissoc key))

(defn reset []
  (reset! mem {}))

(defn get [key]
  (let [{:keys [ttl value]} (clojure.core/get @mem key)]
    (if (or (nil? value) (>= (now 0) ttl))
      (do
        (delete key)
        nil)
      value)))

(defn set [key value]
  (swap! mem assoc key {:value value :ttl (now (* 5 60))}))

(defmacro cache! [key & fns]
  `(if-let [v# (get ~key)]
     v#
     (let [result# (do ~@fns)]
       (set ~key result#)
       result#)))
