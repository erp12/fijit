(ns erp12.fijit.function-test
  (:require [clojure.test :refer :all]
            [expectations.clojure.test :refer :all]
            [erp12.fijit.function :refer :all])
  (:import (java.io ObjectInputStream ObjectOutputStream ByteArrayOutputStream ByteArrayInputStream)))

(deftest scala-fn-test
  (let [f0 (scala-fn [] 100)
        f1 (scala-fn [a] (+ 100 a))
        f2 (->fn2 (fn [a b] (+ a b)))]
    (testing "Scala functions"
      (is (instance? scala.Function0 f0))
      (is (instance? scala.Function1 f1))
      (is (instance? scala.Function2 f2)))
    (testing "Scala functions are callable"
      (expect 100 (f0))
      (expect 100 (.apply f0))
      (expect 101 (f1 1))
      (expect 99 (.apply f1 -1))
      (expect 3 (f2 1 2))
      (expect 2 (.apply f2 1 1)))
    (testing "Scala functions are serializable"
      (doseq [[f caller expected] [[f0 #(.apply %) 100]
                                   [f1 #(.apply % 1) 101]
                                   [f2 #(.apply % -1 1) 0]]]
        (let [bos (ByteArrayOutputStream.)
              oos (ObjectOutputStream. bos)
              _ (.writeObject oos f)
              bis (ByteArrayInputStream. (.toByteArray bos))
              ois (ObjectInputStream. bis)
              actual (.readObject ois)]
          (expect expected (caller actual)))))))
