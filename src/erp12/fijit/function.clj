(ns ^{:doc "Utilities for constructing instances of scala's `Function*` traits.

This namespace defines a suite or types (via `deftype`) that each correspond to a Scala function
with a different arity. They all are constructed with a single argument value: a clojure function
with the same arity.

Each type also implements `Serializable` and `clojure.lang.IFn` so that
instances can be called as normal Clojure functions."}
  erp12.fijit.function
  (:import (java.io Serializable)
           (clojure.lang IFn)))

(deftype Function0 [f]
  scala.Function0
  (apply [_]
    (f))
  IFn
  (invoke [_]
    (f))
  Serializable)

(defn ->fn0
  "Wraps a Clojure function with arity 0 in a Scala `Function0`."
  [f]
  (->Function0 f))

(deftype Function1 [f]
  scala.Function1
  (apply [_ a]
    (f a))
  IFn
  (invoke [_ a]
    (f a))
  Serializable)

(defn ->fn1
  "Wraps a Clojure function with arity 1 in a Scala `Function1`."
  [f]
  (->Function1 f))

(deftype Function2 [f]
  scala.Function2
  (apply [_ a b]
    (f a b))
  IFn
  (invoke [_ a b]
    (f a b))
  Serializable)

(defn ->fn2
  "Wraps a Clojure function with arity 2 in a Scala `Function2`."
  [f]
  (->Function2 f))

(deftype Function3 [f]
  scala.Function3
  (apply [_ a b c]
    (f a b c))
  IFn
  (invoke [_ a b c]
    (f a b c))
  Serializable)

(defn ->fn3
  "Wraps a Clojure function with arity 3 in a Scala `Function3`."
  [f]
  (->Function3 f))

(deftype Function4 [f]
  scala.Function4
  (apply [_ a b c d]
    (f a b c d))
  IFn
  (invoke [_ a b c d]
    (f a b c d))
  Serializable)

(defn ->fn4
  "Wraps a Clojure function with arity 4 in a Scala `Function4`."
  [f]
  (->Function4 f))

(deftype Function5 [f]
  scala.Function5
  (apply [_ a b c d e]
    (f a b c d e))
  IFn
  (invoke [_ a b c d e]
    (f a b c d e))
  Serializable)

(defn ->fn5
  "Wraps a Clojure function with arity 5 in a Scala `Function5`."
  [f]
  (->Function5 f))

(defmacro scala-fn
  {:special-form true
   :forms        '[(scala-fn [params*] exprs*)]
   :style/indent 1
   :doc          "Creates a Scala function using idiomatic Clojure syntax.

   Scala utilizes a different function type (expressed as a trait) depending on the number of arguments,
   and thus the type of the returned value will different depending on the arity of the function being defined.
   "}
  [params & body]
  (let [arity (count params)
        ctor (symbol (str "erp12.fijit.function/->Function" arity))]
    `(~ctor
       (~'fn [~@params]
         ~@body))))
