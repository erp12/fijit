(ns ^{:doc "A suite of functions and macros to help write Clojure code that is agnostic to the
version of Scala on the classpath."}
  erp12.fijit.version
  (:require [clojure.string :as s]
            [clojure.string :as str])
  (:import (scala.util Properties)))

(def scala-version
  "A map describing the version of the Scala language that is currently on the classpath. Composed of 3 integer
  fields under the keys `:major`, `:minor`, and `:patch`."
  (let [[major minor patch] (-> (Properties/versionString)
                                (s/replace #"^version " "")
                                (s/split #"\.")
                                (->> (map #(Integer/parseInt %))))]
    {:major major
     :minor minor
     :patch patch}))


(defn parse-version
  "Given a version keyword, parse it into a map with the keys `:major`, `:minor`, and `:patch` and numeric values.

  Examples:

  ```
  (parse-version :2.12) ; => {:major 2 :minor 12}
  (parse-version :2.13.6) ; => {:major 2 :minor 13 :patch 6}
  ```
  "
  [version-keyword]
  (let [parts (-> version-keyword name (str/split #"\.") (->> (map #(Long/parseLong %))))
        part-names (take (count parts) [:major :minor :patch])]
    (zipmap part-names parts)))


(defn compare-versions
  "Given 2 version map describing versions, return -1 or 1 if `v1` is lower or higher than `v2`. Returns 0 if the
   versions are logically the same.

   Examples:

   ```
   (compare-versions {:major 2 :minor 12 :patch 13} {:major 2 :minor 13}) ;; => -1
   (compare-versions {:major 2 :minor 13 :patch 6} {:major 2 :minor 13}) ;; => 1
   (compare-versions {:major 2} {:major 2 :minor 0}) ;; => 0
   ```
   "
  [v1 v2]
  (loop [v1 [(:major v1) (get v1 :minor 0) (get v1 :patch 0)]
         v2 [(:major v2) (get v2 :minor 0) (get v2 :patch 0)]]
    (cond
      (empty? v1) 0
      (< (first v1) (first v2)) -1
      (> (first v1) (first v2)) 1
      :else (recur (rest v1) (rest v2)))))


(defmacro by-scala-version
  "Macro that executes an appropriate form depending on the version of the Scala language found on the classpath.

  The arguments should be alternating between a version keyword and a form. Version keywords can either take the form
  `:major.minor` or :major.minor.patch`. For example `:2.13` and `:2.12.6` are valid version keywords.

  The form that corresponds to the highest compatible version will be executed. A compatible version
  is one that has the same major and minor versions as the current version of Scala on the classpath. If a form's
  version keyword contains a patch version, it will only be valid if the patch version of the active Scala version
  is at least the same number.

  ```
  (by-scala-version :2.12 :A
                    :2.12.10 :B
                    :2.13.4 :C)
  ```

  For example the above code will

  - Return `:A` on Scala 2.12.0 through 2.13.9.
  - Return `:B` for :2.12.10 and all other 2.12.x versions.
  - Throw and exception for scala 2.13.0 through 2.13.3.
  - Return `:C` for 2.13.4 and all other 2.13.x versions.

  "
  [& versions-and-forms]
  {:pre [(even? (count versions-and-forms))]}
  (let [versions (->> versions-and-forms (take-nth 2) (map #(parse-version (eval %))))
        forms (zipmap versions (->> versions-and-forms rest (take-nth 2)))
        valid-versions (filter (fn [v] (and (= (dissoc scala-version :patch)
                                               (dissoc v :patch))
                                            (if (contains? v :patch)
                                              (>= (:patch scala-version) (:patch v))
                                              true)))
                               versions)]
    (if (empty? valid-versions)
      (throw (ex-info "Could not find compatible form for current Scala version."
                      {:current-version scala-version
                       :form-versions   versions})))
    (get forms
         (apply max-key
                #(+ (* 100000 (get % :major 0))
                    (* 100 (get % :minor 0))
                    (get % :patch 0))
                valid-versions))))
