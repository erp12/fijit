(ns erp12.fijit.collection-test
  (:require [clojure.test :refer :all]
            [expectations.clojure.test :refer :all]
            [erp12.fijit.collection :refer :all])
  (:import (scala.collection.immutable Seq Set Vector List)
           (scala.collection Map Iterator)
           (clojure.lang Keyword)))

(deftest ->clj-test
  (testing "array to clj"
    (let [a (->clj (to-scala-array Keyword [:a :b]))]
      (expect "[Lclojure.lang.Keyword;"
              (.getName (class a)))
      (expect [:a :b] (vec a))))
  (testing "vector to clj"
    (let [a (->clj (scala-vector :a :b))]
      (is (vector? a))
      (expect [:a :b] a))
    (expect [] (->clj (scala-vector))))
  (testing "set to clj"
    (let [a (->clj (scala-set :a :b))]
      (is (set? a))
      (expect #{:a :b} a))
    (expect #{} (->clj (scala-set))))
  (testing "map to clj"
    (let [a (->clj (scala-map :a 1 :b 2))]
      (is (map? a))
      (expect {:a 1 :b 2} a))
    (expect {} (->clj (scala-map))))
  (testing "list to clj"
    (let [a (->clj (scala-list :a :b))]
      (is (list? a))
      (expect (list :a :b) a))
    (expect [] (->clj (scala-list))))
  (testing "seq to clj"
    (let [a (->clj (scala-seq :a :b))]
      (is (seq? a))
      (expect (list :a :b) a))
    (expect '() (->clj (scala-seq))))
  (testing "iterator to clj"
    (let [a (->clj (scala-iter->ju-iter (.iterator (scala-seq :a :b))))]
      (is (instance? java.util.Iterator a))
      (expect [:a :b] (vec (iterator-seq a))))))

(deftest ->scala-test
  (testing "vector to scala"
    (let [a (->scala [:a :b])]
      (is (instance? Vector a))
      (expect (scala-vector :a :b))))
  (testing "set to scala"
    (let [a (->scala #{:a :b})]
      (is (instance? Set a))
      (expect (scala-set :a :b))))
  (testing "map to scala"
    (let [a (->scala {:a 1 :b 2})]
      (is (instance? Map a))
      (expect (scala-map :a 1 :b 2) a)))
  (testing "list to scala"
    (let [a (->scala (list :a :b))]
      (is (instance? List a))
      (expect (scala-list :a :b) a)))
  (testing "iterator to scala"
    (let [a (->scala (.iterator [:a :b]))]
      (is (instance? Iterator a))
      (expect [:a :b] (vec (->clj a)))))
  (testing "other seq to scala"
    (let [a (->scala (list :a :b))]
      (is (instance? Seq a))
      (expect (scala-seq :a :b) a))))
