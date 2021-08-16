(ns erp12.fijit.reflect.tree
  (:refer-clojure :exclude [apply])
  (:require [erp12.fijit.reflect.core :as rc]
            [erp12.fijit.collection :as sc]))

(defn term-name
  [s]
  (-> rc/universe .TermName (.apply (name s))))

(defn ident
  [name]
  (-> rc/universe .Ident (.apply name)))

(defn select
  [qualifier name]
  (-> rc/universe .Select (.apply qualifier name)))

(defn apply
  [func args]
  (-> rc/universe .Apply (.apply func (sc/to-list args))))

(defn type-apply
  [func args]
  (-> rc/universe .TypeApply (.apply func (sc/to-list args))))

;; @todo Add the rest of the *Exractor classes from Trees.scala
