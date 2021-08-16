(ns erp12.fijit.function.util
  (:import (java.io Serializable)))

(defprotocol WrappedIFn
  (func [this]))

(deftype SerializableWrappedIFn [^:unsynchronized-mutable f]
  WrappedIFn
  (func [_] f)
  Serializable)
