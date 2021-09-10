(ns erp12.fijit.try-test
  (:require [clojure.test :refer :all]
            [expectations.clojure.test :refer :all]
            [erp12.fijit.try :as sc-try])
  (:import (clojure.lang ExceptionInfo)))

(deftest get-test
  (testing "Re-throwing improved errors"
    (let [msg "Failed division!"]
      (expect (more-> ExceptionInfo type
                      msg ex-message)
              (get (sc-try/scala-try (/ 1 0))
                   (throw (ex-info msg {:numerator 1 :denominator 0})))))))
