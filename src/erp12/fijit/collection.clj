(ns erp12.fijit.collection
  (:refer-clojure :exclude [to-array])
  (:require [clojure.core :as core])
  (:import (scala.collection.immutable List Set Vector Seq)
           (scala.collection JavaConverters Map Iterator)
           (scala Array)
           (scala.reflect ClassTag)))

; @todo Re-write this namespace so that it deligates properly based on scala version.
; 2.12 --> scala.collection.JavaConverters
; 2.13 --> scala.java.*

(defn to-iterator
  "Converts a Clojure collection to a Scala iterator."
  ^Iterator [coll]
  (-> coll .iterator JavaConverters/asScalaIterator))

(defn to-seq
  "Converts a Clojure collection to a Scala `Seq`."
  ^Seq [coll]
  (-> coll to-iterator .toSeq))

(defn to-list
  "Converts a Clojure collection to a Scala `List`."
  ^List [coll]
  (-> coll to-iterator .toList))

(defn to-set
  "Converts a Clojure collection to a Scala `Set`."
  ^Set [coll]
  (-> coll to-iterator .toSet))

(defn to-vector
  "Converts a Clojure collection to a Scala `Vector`."
  ^Vector [coll]
  (-> coll to-iterator .toVector))

(defn to-scala-array
  "Converts a Clojure collection to a Scala `Array`.

  Like Clojure core `into-array`, all elements must be of the same class (or nil).
  "
  ^Array
  ([coll]
   (to-scala-array (class (first (filter some? coll))) coll))
  ([^Class cls coll]
   (.toArray (to-seq coll) (ClassTag/apply cls))))

(defn to-map
  "Converts a Clojure map to a Scala `Map`."
  ^Map [m]
  (JavaConverters/mapAsScalaMap m))

(defn scala-seq
  "Creates a Scala `Seq`."
  ^Seq [& args]
  (to-seq args))

(defn scala-list
  "Creates a Scala `List`."
  ^List [& args]
  (to-list args))

(defn scala-set
  "Creates a Scala `Set`."
  ^Set [& args]
  (to-set args))

(defn scala-vector
  "Creates a Scala `Vector`."
  ^Vector [& args]
  (to-vector args))

(defn scala-map
  ^Map [& keyvals]
  (to-map (apply array-map keyvals)))

(defn seq->clj
  "Converts a Scala `Seq` to a clojure seq."
  [^Seq s]
  (-> s
      JavaConverters/asJavaIterable
      core/seq))

(defn vector->clj
  "Converts a Scala `Vector` to a clojure vector."
  [^Vector v]
  (core/vec (seq->clj v)))

(defn set->clj
  "Converts a Scala `Set` to a clojure set."
  [^Set s]
  (-> s
      JavaConverters/asJavaIterable
      core/set))

(defn map->clj
  "Converts a Scala `Map` to a clojure map."
  [^Map m]
  (->> m
       JavaConverters/mapAsJavaMap
       (into {})))

(defn scala-iter->ju-iter
  ^java.util.Iterator [^Iterator scala-iter]
  (JavaConverters/asJavaIterator scala-iter))

(defn ju-iter->scala-iter
  ^Iterator [^java.util.Iterator ju-iter]
  (JavaConverters/asScalaIterator ju-iter))

(defn ->clj
  "Converts a Scala collection to its Clojure counterpart.
  Objects that are not supported immutable Scala collections
  are returned unchanged.

  Mapping:
    Scala `ArrayLike` -> Clojure vector
    Scala `Vector` -> Clojure vector
    Scala `Set` -> Clojure set
    Scala `Map` -> Clojure set
    Scala `List` -> Clojure list
    Scala `Seq` -> Clojure list
  "
  [x]
  (cond
    (instance? Array x) x
    (instance? Vector x) (vector->clj x)
    (instance? Set x) (set->clj x)
    (instance? Map x) (map->clj x)
    (instance? List x) (into (list) (reverse (seq->clj x)))  ;; @todo Is this too slow?
    (instance? Seq x) (seq->clj x)
    (instance? Iterator x) (iterator-seq (scala-iter->ju-iter x))
    :else x))

(defn ->scala
  "Converts a Clojure collection to its Scala counterpart.
  Non-collection inputs are returned unchanged.

  Mapping:
    Clojure vector -> Scala `Vector`
    Clojure set -> Scala `Set`
    Clojure map -> Scala `Map`
    Clojure `sequential?` -> Scala `Seq`
  "
  [x]
  (cond
    (vector? x) (to-vector x)
    (set? x) (to-set x)
    (map? x) (to-map x)
    (list? x) (to-list x)
    (instance? java.util.Iterator x) (ju-iter->scala-iter x)
    (sequential? x) (to-seq x)
    :else x))
