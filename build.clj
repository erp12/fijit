(ns build
  (:require [clojure.tools.build.api :as b]
            [org.corfield.build :as bb]))

(def lib 'io.github.erp12/fijit)

(def version (format "1.0.%s" (b/git-count-revs nil)))

(def scala-verions [:2.12 :2.13])

(def target "target")

(def class-dir (format "%s/classes" target))

(def aot-class-dir "classes")

(def jar-file (format "%s/%s-%s.jar" target (name (or lib 'application)) version))

;; Utils ;;;;;;;;;;

(defn for-scala-versions
  [opts f]
  (doseq [scala-ver (if-let [v (:scala-version opts)]
                      [v]
                      scala-verions)]
    (f (merge {:aliases [scala-ver]}
              opts))))


;; Entry ;;;;;;;;;;

(defn tests
  [opts]
  (for-scala-versions opts bb/run-tests))

(defn jar
  [opts]
  (-> opts
      (assoc :lib lib :version version)
      (bb/clean)
      (bb/jar)))

(defn gen-docs
  [_]
  (println "Generating docs")
  (let [{:keys [exit out err]} (b/process {:command-args ["clj" "-X:2.13:codox"]})]
    (println out)
    (println err)
    (when-not (zero? exit)
      (System/exit exit))))

(defn ci
  "Run the CI pipeline. This runs tests for each Scala version and build the jar."
  [opts]
  (tests opts)
  (jar opts))

(defn prepare-release
  [opts]
  (ci opts)
  (gen-docs opts))

(defn publish
  [opts]
  (-> opts
      (assoc :lib lib :version version)
      (ci)
      (bb/deploy)))
