(ns erp12.fijit.reflect.core
  (:import (scala.reflect.runtime JavaUniverse JavaMirrors$JavaMirror)
           (scala.tools.reflect package$)))

(def universe ^JavaUniverse
  (JavaUniverse.))

(def class-loader ^ClassLoader
  (-> (Thread/currentThread) .getContextClassLoader))

(def mirror ^JavaMirrors$JavaMirror
  (.runtimeMirror universe class-loader))

(def toolbox
  (-> package$/MODULE$
      (.ToolBox mirror)
      (.mkToolBox (.mkSilentFrontEnd package$/MODULE$) "")))
