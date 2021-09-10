(ns build
  (:require [clojure.tools.build.api :as b]
            [org.corfield.build :as bb]
            [clojure.java.shell :refer [sh]]))

(def lib 'io.github.erp12/fijit)

(def lib-version (format "0.0.%s" (b/git-count-revs nil)))

(defn tag
  [scala-ver]
  (format "%s-%s" (name scala-ver) lib-version))

(defn jar-file
  [scala-ver]
  (format "target/%s_%s.jar" (name lib) (tag scala-ver)))

(def scala-verions [:2.12 :2.13])

;; Utils ;;;;;;;;;;

(defn for-scala-versions
  [opts f]
   (doseq [scala-ver (if-let [v (:scala-version opts)]
                       [v]
                       scala-verions)]
     (f (merge {:aliases [scala-ver]
                :lib lib
                :version lib-version
                :jar-file (jar-file scala-ver)
                :tag (tag scala-ver)}
               opts))))

;; Entry ;;;;;;;;;;

(defn tests
  [opts]
  (for-scala-versions opts bb/run-tests))

(defn jars
  [opts]
  (bb/clean opts)
  (for-scala-versions opts bb/jar))

(defn gen-docs
  [_]
  (println "Generating docs")
  (let [{:keys [exit out err]} (sh "clj" "-X:2.13:codox")]
    (println out)
    (println err)
    (when-not (zero? exit)
      (System/exit exit))))

(defn ci
  "Run the CI pipeline. This runs tests and builds JARs for each Scala version."
  [opts]
  (tests opts)
  (jars opts))

(defn prepare-release
  [opts]
  (ci opts)
  (gen-docs opts))