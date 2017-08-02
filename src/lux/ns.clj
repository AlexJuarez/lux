(ns lux.ns)

(defmacro import-fn
  "Given a function in another namespace, defines a function with the same name in the
   current namespace.  Argument lists, doc-strings, and original line-numbers are preserved."
  [ns sym]
  (let [vr (ns-resolve ns sym)
        m (meta vr)
        nspace (:name m)
        n (:name m)
        arglists (:arglists m)
        doc (:doc m)
        protocol (:protocol m)]
    (when-not vr
      (throw (IllegalArgumentException. (str "Don't recognize " sym))))
    (when (:macro m)
      (throw (IllegalArgumentException. (str "Calling import-fn on a macro: " sym))))
    `(do
       (def ~(with-meta n {:protocol protocol}) (deref ~vr))
       (alter-meta! (var ~n) assoc
         :doc ~doc
         :arglists ~(list 'quote arglists)
         :file ~(:file m)
         :line ~(:line m))
       ~vr)))

(defmacro import-macro
  "Given a macro in another namespace, defines a macro with the same name in the
   current namespace.  Argument lists, doc-strings, and original line-numbers are preserved."
  [ns sym]
  (let [vr (ns-resolve ns sym)
        m (meta vr)
        n (:name m)
        nspace (:ns m)
        arglists (:arglists m)
        doc (:doc m)]
    (when-not vr
      (throw (IllegalArgumentException. (str "Don't recognize " sym))))
    (when-not (:macro m)
      (throw (IllegalArgumentException. (str "Calling import-macro on a non-macro: " sym))))
    `(do
       (def ~n ~(ns-resolve ns sym))
       (alter-meta! (var ~n) assoc
         :doc ~doc
         :arglists ~(list 'quote arglists)
         :file ~(:file m)
         :line ~(:line m))
       (.setMacro (var ~n))
       ~vr)))

(defmacro import-def
  "Given a regular def'd var from another namespace, defined a new var with the
   same name in the current namespace."
  [ns sym]
  (let [vr (ns-resolve ns sym)
        m (meta vr)
        n (:name m)
        n (if (:dynamic m) (with-meta n {:dynamic true}) n)
        nspace (:ns m)
        doc (:doc m)]
    (when-not vr
      (throw (IllegalArgumentException. (str "Don't recognize " sym))))
    `(do
       (def ~n @~vr)
       (alter-meta! (var ~n) assoc
         :doc ~doc
         :file ~(:file m)
         :line ~(:line m))
       ~vr)))

(defmacro import [ns sym]
  (let [vr (ns-resolve ns sym)
        m (meta vr)]
    (cond
      (:macro m) `(import-macro ~ns ~sym)
      (:arglists m) `(import-fn ~ns ~sym)
      :default `(import-def ~ns ~sym))))

(defmacro import-all [ns]
  (require ns)
  `(do ~@(for [i (map first (ns-publics ns))]
           `(import ~ns ~i))))
