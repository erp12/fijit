(ns erp12.fijit.option
  (:refer-clojure :exclude [get])
  (:import [scala Option]))


(def none ^Option
  (. Option empty))


(defn to-some ^Option
  [v]
  (. Option apply v))


(defn option ^Option
  ([]
   none)
  ([value]
   (if (nil? value)
     none
     (to-some value))))


(defn is-empty ^Boolean
  [^Option opt]
  (. opt isEmpty))


(defn is-defined ^Boolean
  [^Option opt]
  (. opt isDefined))


(defmacro get
  ([^Option opt]
   `(. ~opt get))
  ([^Option opt or-else]
   `(if (is-defined ~opt)
      (. ~opt get)
      ~or-else)))


(defn get-or-nil
  [^Option opt]
  (get opt nil))
