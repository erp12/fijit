(ns ^{:doc "Utilities for creating and unpacking instances of scala's `Tuple*` classes."}
  erp12.fijit.tuple
  (:require [erp12.fijit.collection :as sc])
  (:import (scala Product)))

(defn tuple
  "Creates a Scala `Tuple` from the elements.

  Scala uses a different `Tuple` class depending on the number of elements and thus the type of the returned value
  will different depending on the number of arguments.
  "
  ([a]
   (scala.Tuple1. a))
  ([a b]
   (scala.Tuple2. a b))
  ([a b c]
   (scala.Tuple3. a b c))
  ([a b c d]
   (scala.Tuple4. a b c d))
  ([a b c d e]
   (scala.Tuple5. a b c d e))
  ([a b c d e f]
   (scala.Tuple6. a b c d e f))
  ([a b c d e f g]
   (scala.Tuple7. a b c d e f g))
  ([a b c d e f g h]
   (scala.Tuple8. a b c d e f g h))
  ([a b c d e f g h i]
   (scala.Tuple9. a b c d e f g h i))
  ([a b c d e f g h i j]
   (scala.Tuple10. a b c d e f g h i j))
  ([a b c d e f g h i j k]
   (scala.Tuple11. a b c d e f g h i j k))
  ([a b c d e f g h i j k l]
   (scala.Tuple12. a b c d e f g h i j k l))
  ([a b c d e f g h i j k l m]
   (scala.Tuple13. a b c d e f g h i j k l m))
  ([a b c d e f g h i j k l m n]
   (scala.Tuple14. a b c d e f g h i j k l m n))
  ([a b c d e f g h i j k l m n o]
   (scala.Tuple15. a b c d e f g h i j k l m n o))
  ([a b c d e f g h i j k l m n o p]
   (scala.Tuple16. a b c d e f g h i j k l m n o p))
  ([a b c d e f g h i j k l m n o p q]
   (scala.Tuple17. a b c d e f g h i j k l m n o p q))
  ([a b c d e f g h i j k l m n o p q r]
   (scala.Tuple18. a b c d e f g h i j k l m n o p q r))
  ([a b c d e f g h i j k l m n o p q r s]
   (scala.Tuple19. a b c d e f g h i j k l m n o p q r s))
  ([a b c d e f g h i j k l m n o p q r s t]
   (scala.Tuple20. a b c d e f g h i j k l m n o p q r s t))
  ([a b c d e f g h i j k l m n o p q r s t & [u v :as args]]
   (case (count args)
     1 (scala.Tuple21. a b c d e f g h i j k l m n o p q r s t u)
     2 (scala.Tuple22. a b c d e f g h i j k l m n o p q r s t u v)
     (throw (ex-info "Can only create Scala tuples with up to 22 elements"
                     {:count (+ 20 (count args))})))))

(defn to-tuple
  "Converts the collection to a scala Tuple.

  Scala uses a different `Tuple` class depending on the number of elements and thus the type of the returned value
  will different depending on the size of the collection.
  "
  [coll]
  (apply tuple coll))

(defn product->vec
  "Converts a Scala `Product` type (for example, any instance of any `Tuple` class) to a Clojure vector."
  [^Product tup]
  (->> tup .productIterator .toSeq sc/->clj vec))
