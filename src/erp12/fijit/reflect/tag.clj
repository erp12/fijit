(ns erp12.fijit.reflect.tag
  (:require [erp12.fijit.reflect.core :as rc])
  (:import (scala.reflect.api TypeTags$TypeTag)
           (scala.reflect ClassTag)))

(def byte-cls-tag (ClassTag/Byte))
(def short-cls-tag (ClassTag/Short))
(def char-cls-tag (ClassTag/Char))
(def int-cls-tag (ClassTag/Int))
(def long-cls-tag (ClassTag/Long))
(def float-cls-tag (ClassTag/Float))
(def double-cls-tag (ClassTag/Double))
(def boolean-cls-tag (ClassTag/Boolean))
(def unit-cls-tag (ClassTag/Unit))
(def any-cls-tag (ClassTag/Any))
(def any-val-cls-tag (ClassTag/AnyVal))
(def any-ref-cls-tag (ClassTag/AnyRef))
(def object-cls-tag (ClassTag/Object))
(def nothing-cls-tag (ClassTag/Nothing))
(def null-cls-tag (ClassTag/Null))

(defn class-tag
  [cls]
  (ClassTag/apply cls))

(defn compile-type-tag
  [cls]
  (.eval rc/toolbox (.parse rc/toolbox (format "scala.reflect.runtime.universe.typeTag[%s]" (.getName cls)))))

(def byte-tt (-> rc/universe .TypeTag .Byte))
(def short-tt (-> rc/universe .TypeTag .Short))
(def char-tt (-> rc/universe .TypeTag .Char))
(def int-tt (-> rc/universe .TypeTag .Int))
(def long-tt (-> rc/universe .TypeTag .Long))
(def float-tt (-> rc/universe .TypeTag .Float))
(def double-tt (-> rc/universe .TypeTag .Double))
(def boolean-tt (-> rc/universe .TypeTag .Boolean))
(def unit-tt (-> rc/universe .TypeTag .Unit))
(def any-tt (-> rc/universe .TypeTag .Any))
(def any-val-tt (-> rc/universe .TypeTag .AnyVal))
(def any-ref-tt (-> rc/universe .TypeTag .AnyRef))
(def object-tt (-> rc/universe .TypeTag .Object))
(def nothing-tt (-> rc/universe .TypeTag .Nothing))
(def null-tt (-> rc/universe .TypeTag .Null))

(def -simple-cls-tt-map
  {Byte      byte-tt
   Short     short-tt
   Character char-tt
   Integer   int-tt
   Long      long-tt
   Float     float-tt
   Double    double-tt
   Boolean   boolean-tt
   Object    object-tt
   nil       null-tt})

(def -memo-compile-type-tag
  (memoize compile-type-tag))

(defn type-tag ^TypeTags$TypeTag
  [^Class cls]
  (or (-simple-cls-tt-map cls)
      (-memo-compile-type-tag cls)))


;; For creating TypeTags that cannot be migrated.
;; Faster than using the compiler, but more limited.
;(let [mirror (.runtimeMirror rc/universe (.getClassLoader cls))
;      typ (scala-type cls)
;      type-creator (proxy [TypeCreator] []
;                     (apply [m]
;                       (if (= m mirror)
;                         typ
;                         (throw (ex-info "Type tag cannot be migrated to other mirrors."
;                                         {:typ            typ
;                                          :default-mirror mirror
;                                          :new-mirror     m})))))]
;  (-> rc/universe .TypeTag (.apply mirror type-creator)))
