(ns erp12.fijit.tuple-test
  (:require [clojure.test :refer :all]
            [expectations.clojure.test :refer :all]
            [erp12.fijit.tuple :refer :all]))

(deftest tuple-test
  (testing "Tuple creation"
    (let [tup (scala-tuple 1 :a "")]
      (expect 1 (._1 tup))
      (expect :a (._2 tup))
      (expect "" (._3 tup)))
    ;; Create tuples from sequential collections.
    (expect (scala-tuple :a 1) (to-tuple [:a 1]))
    (expect (scala-tuple 1 "A") (to-tuple '(1 "A"))))
  (testing "Converting tuples to vectors"
    (expect (product->vec (scala-tuple 1 :a ""))
            [1 :a ""])))
