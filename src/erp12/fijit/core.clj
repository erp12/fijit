(ns erp12.fijit.core
  (:import (scala.runtime BoxedUnit)))

(def unit
  "The only instance of `scala.Unit`. Written as `()` in Scala.

  See: https://docs.scala-lang.org/tour/unified-types.html
  See: https://en.wikipedia.org/wiki/Unit_type"
  (BoxedUnit/UNIT))