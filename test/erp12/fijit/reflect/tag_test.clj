(ns erp12.fijit.reflect.tag-test
  (:require [clojure.test :refer :all]
            [expectations.clojure.test :refer :all]
            [erp12.fijit.reflect.tag :refer :all])
  (:import (java.time LocalDate)))

(deftest test-type-tag
  (expect "TypeTag[String]" (.toString (type-tag String)))
  (expect "TypeTag[java.time.LocalDate]" (.toString (type-tag LocalDate))))
