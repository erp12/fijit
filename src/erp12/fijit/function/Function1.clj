(ns erp12.fijit.function.Function1
  (:gen-class
    :implements [scala.Function1
                 java.io.Serializable]
    :state state
    :init init
    :constructors {[clojure.lang.IFn] []})
  (:require [erp12.fijit.function.util :as fu]))

(defn -init
  [f]
  [[] (fu/->SerializableWrappedIFn f)])

(defn -apply
  [this a]
  (let [f (.func (.state this))]
    (f a)))
