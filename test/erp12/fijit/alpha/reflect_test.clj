(ns erp12.fijit.alpha.reflect-test
  (:require [clojure.test :refer :all]
            [expectations.clojure.test :refer :all]
            [erp12.fijit.alpha.reflect :as r]
            [erp12.fijit.collection :as sc]
            [erp12.fijit.version :as ver])
  (:import (java.time LocalDate)
           (scala.collection.immutable Map Seq List)
           (scala.reflect ClassTag)))

(def string
  (r/scala-type String))

(def option-long
  (r/scala-type scala.Option [Long]))

(def string->ints
  (r/scala-type Map [String (r/scala-type Seq [Integer])]))

(deftest test-scala-types
  (testing "creating types"
    (expect "java.lang.String" (r/full-type-pr string))
    (expect "scala.Option[java.lang.Long]" (r/full-type-pr option-long))
    (expect "scala.collection.immutable.Map[java.lang.String,scala.collection.immutable.Seq[java.lang.Integer]]" (r/full-type-pr string->ints)))
  (testing "querying type"
    (testing "bases"
      (expect '("class String" "trait CharSequence" "trait Comparable" "trait Serializable" "class Object" "class Any")
              (map str (r/base-classes string)))
      (expect (ver/by-scala-version :2.12 '("class Option" "trait Serializable" "trait Serializable" "trait Product" "trait Equals" "class Object" "class Any")
                                    :2.13 '("class Option" "trait Serializable" "trait Product" "trait Equals" "trait IterableOnce" "class Object" "class Any"))
              (map str (r/base-classes option-long))))
    (testing "type args and params"
      (expect '() (r/type-args string))
      (expect '("java.lang.Long") (map r/full-type-pr (r/type-args option-long)))
      (expect '() (map r/full-type-pr (r/type-params option-long)))
      (expect '("scala.collection.immutable.Seq.A") (map r/full-name (r/type-params (r/scala-type Seq)))))
    (testing "members"
      (let [decl-strs (set (map str (r/decls option-long)))]
        (is (every? #(contains? decl-strs %) ["method get" "method isEmpty" "method isDefined" "constructor Option"])))
      (expect "scala.Option.get"
              (r/full-name (r/decl option-long (r/term-name "get"))))))
  (testing "comparing type"
    (testing "conforms"
      (is (r/conforms? (r/scala-type List [String]) (r/scala-type Seq [String])))
      (is (not (r/conforms? (r/scala-type Seq [String]) (r/scala-type List [String]))))
      (is (r/weak-conforms? (r/scala-type scala.Int) (r/scala-type scala.Long))))))

(deftest test-tags
  (testing "class tags"
    (is (instance? ClassTag r/double-cls-tag)))
  (testing "type tags"
    (expect "TypeTag[String]" (.toString (r/type-tag String)))
    (expect "TypeTag[java.time.LocalDate]" (.toString (r/type-tag LocalDate)))
    (expect "TypeTag[Option[Long]]" (.toString (r/type-tag option-long)))))

(deftest test-tree
  (testing "simple class def"
    (expect "class Foo"
            (let [tree (r/class-def {:name (r/type-name "Foo")})]
              (r/pr-code tree {})))
    (expect "class Point extends java.io.Serializable {\n  val x: Double = 1.0;\n  val y: Double = 1.0;\n  def magnitude = Math.sqrt(Math.plus(Math.sqr(x), Math.sqr(y)))\n}"
            (let [tree (r/class-def {:name (r/type-name "Point")
                                     :impl (r/template {:parents [(r/type->tree (r/scala-type java.io.Serializable))]
                                                        :body    [r/default-constructor
                                                                  (r/val-def {:name (r/term-name "x")
                                                                              :tpt  (r/type->tree (r/scala-type Double))
                                                                              :rhs  (r/literal (r/constant 1.0))})
                                                                  (r/val-def {:name (r/term-name "y")
                                                                              :tpt  (r/type->tree (r/scala-type Double))
                                                                              :rhs  (r/literal (r/constant 1.0))})
                                                                  (r/def-def {:name (r/term-name "magnitude")
                                                                              :rhs  (r/apply (r/select (r/ident (r/term-name "Math")) (r/term-name "sqrt"))
                                                                                             [(r/apply (r/select (r/ident (r/term-name "Math")) (r/term-name "plus"))
                                                                                                       [(r/apply (r/select (r/ident (r/term-name "Math")) (r/term-name "sqr"))
                                                                                                                 [(r/ident (r/term-name "x"))])
                                                                                                        (r/apply (r/select (r/ident (r/term-name "Math")) (r/term-name "sqr"))
                                                                                                                 [(r/ident (r/term-name "y"))])])])})]})})]
              (r/pr-code tree {}))))
  (testing "type params"
    (expect "class Box[T] {\n  def boxed: T\n}"
            ;; @todo Change to `trait`
            (let [tree (r/class-def {:name    (r/type-name "Box")
                                     :tparams [(r/type-def {:name (r/type-name "T")})]
                                     :impl    (r/template {:body [r/default-constructor
                                                                  (r/def-def {:name (r/term-name "boxed")
                                                                              :tpt  (r/ident (r/type-name "T"))
                                                                              :rhs  r/empty-tree})]})})]
              (r/pr-code tree {})))))

(deftest test-symbol
  (is (r/type? (r/type-symbol option-long)))
  (is (r/method? (r/decl option-long (r/term-name "get")))))

(deftest test-mirror
  (testing "instantiate and access field"
    (let [t (r/scala-type scala.Tuple1 [String])
          cm (-> t r/type-symbol r/as-class r/reflect-class)
          ctor (-> cm (r/reflect-constructor (-> t (r/decl r/constructor) r/as-method)))
          instance (.apply ctor (sc/scala-seq "Hi"))
          im (r/reflect instance)
          _1 (r/reflect-field im (-> t (r/decl (r/term-name "_1")) r/as-term))]
      (expect "Hi" (.get _1))))
  (testing "call module method"))
