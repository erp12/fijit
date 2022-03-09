(ns ^{:doc "Utilities for converting collections between Clojure and Scala."}
  erp12.fijit.collection
  (:require [clojure.core :as core]
            [erp12.fijit.version :as sm])
  (:import (scala Array package$ Predef$ Tuple2 Array$)
           (scala.collection Map Iterator)
           (scala.collection.immutable List Set Vector Seq)
           (scala.reflect ClassTag)))


(defn scala-iter->ju-iter
  ^java.util.Iterator [^Iterator scala-iter]
  (sm/by-scala-version :2.12 (scala.collection.JavaConverters/asJavaIterator scala-iter)
                       :2.13 (.asJava (scala.jdk.CollectionConverters/IteratorHasAsJava scala-iter))))

(defn ju-iter->scala-iter
  ^Iterator [^java.util.Iterator ju-iter]
  (sm/by-scala-version :2.12 (scala.collection.JavaConverters/asScalaIterator ju-iter)
                       :2.13 (.asScala (scala.jdk.CollectionConverters/IteratorHasAsScala ju-iter))))


(defn to-scala-iterator
  "Converts a Clojure collection to a Scala iterator."
  ^Iterator [coll]
  (-> coll .iterator (ju-iter->scala-iter)))

(defn to-scala-seq
  "Converts a Clojure collection to a Scala `Seq`."
  ^Seq [coll]
  (-> coll to-scala-iterator .toSeq))

(defn to-scala-list
  "Converts a Clojure collection to a Scala `List`."
  ^List [coll]
  (-> coll to-scala-iterator .toList))

(defn to-scala-set
  "Converts a Clojure collection to a Scala `Set`."
  ^Set [coll]
  (-> coll to-scala-iterator .toSet))

(defn to-scala-vector
  "Converts a Clojure collection to a Scala `Vector`."
  ^Vector [coll]
  (-> coll to-scala-iterator .toVector))

(defn to-scala-array
  "Converts a Clojure collection to a Scala `Array`.

  Like Clojure core `into-array`, all elements must be of the same class (or nil).
  "
  ^Array
  [^Class cls coll]
  (.toArray (to-scala-seq coll) (ClassTag/apply cls)))

(defn to-scala-map
  "Converts a Clojure map to a Scala `Map`."
  ^Map [m]
  (sm/by-scala-version :2.12 (scala.collection.JavaConverters/mapAsScalaMap m)
                       :2.13 (.asScala (scala.jdk.CollectionConverters/MapHasAsScala m))))

(defn scala-seq
  "Creates a Scala `Seq`."
  ^Seq [& args]
  (to-scala-seq (if (nil? args) '() args)))

(defn scala-list
  "Creates a Scala `List`."
  ^List [& args]
  (.apply (.List package$/MODULE$) (to-scala-seq (or args []))))

(defn scala-set
  "Creates a Scala `Set`."
  ^Set [& args]
  (.apply (.Set Predef$/MODULE$) (to-scala-seq (or args []))))

(defn scala-vector
  "Creates a Scala `Vector`."
  ^Vector [& args]
  (.apply (.Vector package$/MODULE$) (to-scala-seq (or args []))))

(defn scala-map
  ^Map [& keyvals]
  (->> keyvals
       (apply array-map)
       (map #(Tuple2. (first %) (second %)))
       to-scala-seq
       (.apply (.Map Predef$/MODULE$))))

(defn scala-array
  ^Array [^Class cls & args]
  (.apply Array$/MODULE$
          (to-scala-seq (or args []))
          (ClassTag/apply cls)))

(defn seq->clj
  "Converts a Scala `Seq` to a clojure seq."
  [^Seq s]
  (if (.isEmpty s)
    '()
    (-> (sm/by-scala-version :2.12 (scala.collection.JavaConverters/asJavaIterable s)
                             :2.13 (.asJava (scala.jdk.CollectionConverters/SeqHasAsJava s)))
        core/seq)))

(defn vector->clj
  "Converts a Scala `Vector` to a clojure vector."
  [^Vector v]
  (core/vec (seq->clj v)))

(defn set->clj
  "Converts a Scala `Set` to a clojure set."
  [^Set s]
  (-> (sm/by-scala-version :2.12 (scala.collection.JavaConverters/asJavaIterable s)
                           :2.13 (.asJava (scala.jdk.CollectionConverters/SetHasAsJava s)))
      core/set))

(defn map->clj
  "Converts a Scala `Map` to a clojure map."
  [^Map m]
  (->> (sm/by-scala-version :2.12 (scala.collection.JavaConverters/mapAsJavaMap m)
                            :2.13 (.asJava (scala.jdk.CollectionConverters/MapHasAsJava m)))
       (into {})))

(defn ->clj
  "Converts a Scala collection to its Clojure counterpart.
  Objects that are not one of the supported immutable Scala collections
  are returned unchanged.

  Mapping:

    - Scala `Vector` -> Clojure vector
    - Scala `Set` -> Clojure set
    - Scala `Map` -> Clojure map
    - Scala `List` -> Clojure list
    - Scala `Seq` -> Clojure seq
    - Scala `Iterator` -> Clojure seq
  "
  [x]
  (cond
    ;(instance? Array x) (vec x) ; Probably best to leave arrays as arrays and not assume a Clojure collection type.
    (instance? Vector x) (vector->clj x)
    (instance? Set x) (set->clj x)
    (instance? Map x) (map->clj x)
    (instance? List x) (into (list) (reverse (seq->clj x))) ;; @todo Is this too slow?
    (instance? scala.collection.Seq x) (seq->clj x)
    (instance? Iterator x) (iterator-seq (scala-iter->ju-iter x))
    :else x))

(defn ->scala
  "Converts a Clojure collection to its Scala counterpart.
  Non-collection inputs are returned unchanged.

  Mapping:

    - Clojure vector -> Scala `Vector`
    - Clojure set -> Scala `Set`
    - Clojure map -> Scala `Map`
    - Clojure `sequential?` -> Scala `Seq`
  "
  [x]
  (cond
    (vector? x) (to-scala-vector x)
    (set? x) (to-scala-set x)
    (map? x) (to-scala-map x)
    (list? x) (to-scala-list x)
    (instance? java.util.Iterator x) (ju-iter->scala-iter x)
    (sequential? x) (to-scala-seq x)
    :else x))
