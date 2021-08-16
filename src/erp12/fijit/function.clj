(ns erp12.fijit.function)

(defmacro scala-fn
  {:special-form true
   :forms        '[(scala-fn [params*] exprs*)]
   :style/indent 1}
  [params & body]
  (let [arity (count params)
        ctor (symbol (str "erp12.fijit.function.Function" arity "."))]
    `(~ctor
       (~'fn [~@params]
         ~@body))))
