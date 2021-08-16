(ns build
  (:require [clojure.tools.build.api :as b]
            ;; Require s3-transporter to eliminate race condition
            ;; https://ask.clojure.org/index.php/10905/control-transient-deps-that-compiled-assembled-into-uberjar?show=10913#c10913
            [clojure.tools.deps.alpha.util.s3-transporter]))

(def lib 'io.github.erp12/fijit)

(def class-dir "target/classes")

(defn jar-file
  [v]
  (format "target/%s-%s.jar" (name lib) v))

;; Entry ;;;;;;;;;;

(defn clean
  [_]
  (println "Cleaning...")
  (b/delete {:path "target"}))

(defn jar
  [{:keys [scala-ver] :as params}]
  (let [jar-basis (b/create-basis {:project "deps.edn"})
        version (format "%s.%s" (name scala-ver) (b/git-count-revs nil))
        jf (jar-file version)]
    (println "Clearing" class-dir)
    (b/delete {:path class-dir})
    (println "Writing pom.xml...")
    (b/write-pom {:class-dir class-dir
                  :lib lib
                  :version version
                  :basis jar-basis
                  :src-dirs ["src"]})
    (println "Copying and compiling src...")
    (b/copy-dir {:src-dirs ["src"]
                 :target-dir class-dir})
    (b/delete {:path (str class-dir "/erp12/fijit/function")})
    (b/compile-clj {:basis (b/create-basis {:project "deps.edn"
                                            :aliases [scala-ver]})
                    :src-dirs ["src"]
                    :class-dir class-dir
                    :ns-compile ['erp12.fijit.function.Function0
                                 'erp12.fijit.function.Function1
                                 'erp12.fijit.function.Function2
                                 'erp12.fijit.function.Function3
                                 'erp12.fijit.function.util]})
    (println (str "Building jar " jf "..."))
    (b/jar {:class-dir class-dir
            :jar-file jf})))

(defn build-all
      [_]
      (clean nil)
      (doseq [scala-ver [:2.12]]
             (println "\nBuild @ Scala" scala-ver)
             (jar {:scala-ver scala-ver})))
