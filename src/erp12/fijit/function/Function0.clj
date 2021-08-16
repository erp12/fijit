(ns erp12.fijit.function.Function0
  (:gen-class
    :implements [scala.Function0
                 java.io.Serializable]
    :state state
    :init init
    :constructors {[clojure.lang.IFn] []})
  (:require [erp12.fijit.function.util :as fu]))

(defn -init
  [f]
  [[] (fu/->SerializableWrappedIFn f)])

(defn -apply
  [this]
  (let [f (.func (.state this))]
    (f)))
