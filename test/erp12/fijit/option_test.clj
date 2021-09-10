(ns erp12.fijit.option-test
  (:require [clojure.test :refer :all]
            [expectations.clojure.test :refer :all]
            [erp12.fijit.option :as o])
  (:import (clojure.lang ExceptionInfo)
           (java.util NoSuchElementException)))

(deftest option-test
  (testing "None"
    (instance? scala.None o/none)
    (expect o/none (o/option nil)))
  (testing "create Some and unpack"
    (let [opt (o/option 5)]
      (instance? scala.Some opt)
      (expect 5 (o/get opt))
      (is (o/defined? opt))
      (is (not (o/empty? opt)))))
  (testing "get"
    (expect :value (o/get (o/option :value)))
    (expect NoSuchElementException (o/get o/none)))
  (testing "get or else"
    (expect :default (o/get o/none :default))
    (expect nil (o/get-or-nil o/none)))
  (testing "get or throw"
    (expect :safe (o/get (o/option :safe)))
    (expect ExceptionInfo (o/get o/none (throw (ex-info "!" {}))))))
