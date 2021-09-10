(ns ^{:doc "Idiomatic Clojure wrapper around the `scala.util.Try` abstraction."}
  erp12.fijit.try
  (:refer-clojure :exclude [get])
  (:import (scala.util Try Success Failure)
           (scala.util.control NonFatal)))

(defmacro scala-try
  ^{:doc "Evaluates the `form`, and wraps the result in a `Try` object. If an exception was raised
  during the evaluation of the `form`, it will be wrapped in a `Failure` and otherwise the returned
  value will be wrapped in a `Success`."
    :tag Try}
  [form]
  `(try
     (Success. ~form)
     (catch Throwable e#
       (if (NonFatal/apply e#)
         (Failure. e#)
         (throw e#)))))

(defn failure?
  "Checks if the `try` is a failure. Returns `false` if `try` is not an instance of `scala.Try`."
  ^Boolean [^Try try]
  (if (instance? Try try)
    (.isFailure try)
    false))

(defn success?
  "Checks if the `try` is a success. Returns `false` if `try` is not an instance of `scala.Try`."
  ^Boolean [^Try try]
  (if (instance? Try try)
    (.isSuccess try)
    false))

(defmacro get
  "Returns the value from `try` if it is a `Success` or throws the exception if it is a `Failure`.

  If an `or-else` form is provided, it will be evaluated in the event the `try` is a failure instead
  of throwing the exception. This can be used to recover from failure or to throw a better error.

  Example:

  ```
  (defn my-div
    [n d]
    (get (scala-try (/ n d))
         (throw (ex-info \"Failed division!\" {:numerator n :denominator d}))))

  (my-div 3 0)
  ; => clojure.lang.ExceptionInfo: Failed division! {:numerator 3, :denominator 0}
  ```"
  ([^Try try]
   `(. ~try get))
  ([^Try try or-else]
   `(if (success? ~try)
      (. ~try get)
      ~or-else)))
