(ns erp12.fijit.function.Function3
  (:gen-class
    :implements [scala.Function3
                 java.io.Serializable]
    :state state
    :init init
    :constructors {[clojure.lang.IFn] []})
  (:require [erp12.fijit.function.util :as fu]))

(defn -init
  [f]
  [[] (fu/->SerializableWrappedIFn f)])

(defn -apply
  [this a b c]
  (let [f (.func (.state this))]
    (f a b c)))
