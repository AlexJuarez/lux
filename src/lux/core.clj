(ns lux.core)

(defmacro bind-all [ns]
  `(do ~@(for [[sym var] (ns-publics ns)]
           `(def ~sym ~var))))
