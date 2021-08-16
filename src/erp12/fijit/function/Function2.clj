(ns erp12.fijit.function.Function2
  (:gen-class
    :implements [scala.Function2
                 java.io.Serializable]
    :state state
    :init init
    :constructors {[clojure.lang.IFn] []})
  (:require [erp12.fijit.function.util :as fu]))

(defn -init
  [f]
  [[] (fu/->SerializableWrappedIFn f)])

(defn -apply
  [this a b]
  (let [f (.func (.state this))]
    (f a b)))
