(ns ^{:doc "Idiomatic Clojure wrapper around the `scala.Option` abstraction."}
  erp12.fijit.option
  (:refer-clojure :exclude [get empty?])
  (:import (scala Option)))


(def ^{:doc "An instance of `scala.None`."
       :tag Option}
  none
  (. Option empty))


(defn option
  {:doc "Create a `scala.Option`. If no value is given, or the value is `nil` a `scala.None`
  will be returned. Otherwise the result will be a `scala.Some` that wraps the given value."
   :tag Option}
  ([]
   none)
  ([value]
   (. Option apply value)))


(defn empty?
  {:doc "Returns true if the given option is empty (aka is a `None`). False otherwise."
   :tag Boolean}
  [^Option opt]
  (. opt isEmpty))


(defn defined?
  {:doc "Returns true if the given option is not empty (aka is a `Some`).  False otherwise."
   :tag Boolean}
  [^Option opt]
  (. opt isDefined))


(defmacro get
  "Gets the value of the `Option`.

  If the `or-else` form is not provide and the Option is empty (aka a `None` object)
  a `NoSuchElementException` will be thrown. If `or-else` is provided, the form will be
  evaluated in the case of a `None`.

  Examples:
  ```
  (get (option 1)) ; => 1
  (get none) ; throws NoSuchElementException
  (get none :default) ; => :default
  (get none (throw (ex-info \"Oh no!\" {}))) ; throws ExceptionInfo
  ```
  "
  ([^Option opt]
   `(. ~opt get))
  ([^Option opt or-else]
   `(if (defined? ~opt)
      (. ~opt get)
      ~or-else)))


(defn get-or-nil
  "Get's the value of the `Option` or returns `nil` if option is empty (aka `None`)."
  [^Option opt]
  (get opt nil))
