;;  Copyright (c) Martin Trojer. All rights reserved.
;;  The use and distribution terms for this software are covered by the
;;  Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;;  which can be found in the file epl-v10.html at the root of this distribution.
;;  By using this software in any fashion, you are agreeing to be bound by
;;  the terms of this license.
;;  You must not remove this notice, or any other, from this software.

(ns frinj.test.utils
  (:use [clojure.test])
  (:use [frinj.core])  
  (:import [frinj.core fjv])
  (:use [frinj.utils]))

(defn- utils-test-fixture [f]
  (reset-states!)
  (dosync
   (alter prefixes #(assoc % "d" (fjv. 1/10 {})))
   (alter prefixes #(assoc % "c" (fjv. 1/100 {})))
   (alter standalone-prefixes #(assoc % "kilo" (fjv. 1000 {})))
   (alter prefixes #(assoc % "k" (fjv. 1000 {})))
   (alter units #(assoc % "m" (fjv. 1 {})))
   (alter units #(assoc % "s" (fjv. 1 {})))
   (alter units #(assoc % "pi" (fjv. 3.14 {})))
   (alter units #(assoc % "inch" (fjv. 127/5000 {"m" 1})))
   (alter fundamental-units #(assoc % {"m" 1} (fjv. 1 {})))
   (alter fundamentals #(conj % "m"))
   (alter fundamentals #(conj % "s")))
  (f))

(use-fixtures :once utils-test-fixture)

(deftest pfx?
  (is (prefix? "d"))
  (is (prefix? "kilo"))
  (is (not (prefix? "kalle")))
  )

(deftest all-pfx
  (is (= #{ "d" "c" "kilo" "k"}    (into #{} (all-prefix-names))))
  )

(deftest lookup-pfx
  (is (= (fjv. 1/10 {})            (lookup-prefix "d")))
  (is (= (fjv. 1000 {})            (lookup-prefix "kilo")))
  (is (nil?                        (lookup-prefix "kalle")))
  )

(deftest resolve-pfxed-unit
  (is (= [(fjv. 1/100 {}) "m"]     (resolve-prefixed-unit "cm")))
  (is (= [one "cK"]                (resolve-prefixed-unit "cK")))
  (is (= [one "centim"]            (resolve-prefixed-unit "centim")))
  (is (= [one "pi"]                (resolve-prefixed-unit "pi")))
  )

(deftest resolve-unit-pfxs
  (is (= one                       (resolve-unit-prefixes {})))
  (is (= (fjv. 1 {"m" 1})          (resolve-unit-prefixes {"m" 1})))
  (is (= (fjv. 1/100 {"m" 1})      (resolve-unit-prefixes {"cm" 1})))
  (is (= (fjv. 1 {"m" 1 "s" -1})   (resolve-unit-prefixes {"cm" 1 "cs" -1})))
  (is (= (fjv. 10 {"s" -1})        (resolve-unit-prefixes {"ds" -1})))
  )

(deftest norm-units
  (is (= one                       (normalize-units one)))
  (is (= (fjv. 1 {"m" 1 "s" -1}))) (normalize-units (fjv. 1 {"m" 1 "s" -1}))
  (is (= (fjv. 3.14 {})            (normalize-units (fjv. 1 {"pi" 1}))))
  (is (= (fjv. (* 3.14 3.14) {})   (normalize-units (fjv. 1 {"pi" 2}))))
  (is (= (fjv. (/ 1 3.14) {})      (normalize-units (fjv. 1 {"pi" -1}))))
  (is (= (fjv. (/ 1 (* 3.14 3.14)){})
         (normalize-units (fjv. 1 {"pi" -2}))))
  (is (= (fjv. 3.14 {"m" 2})       (normalize-units (fjv. 1 {"pi" 1, "m" 2}))))
  (is (= (fjv. 1000 {"m" 1})       (normalize-units (fjv. 1 {"kilo" 1, "m" 1}))))
  )

(deftest r-n-m
  (is (= one                       (resolve-and-normalize-units {})))
  (is (= (fjv. 3140.0 {"m" 1})     (resolve-and-normalize-units {"pi" 1 "km" 1})))
  )

(deftest conv
  (is (= one                       (convert one {})))
  (is (= (fjv. 3.14 {"m" 0 "s" 0}) (convert (fjv. 3.14 {"m" 1 "s" -1})
                                            {"m" 1 "s" -1})))
  (is (= (fjv. 1/100 {"m" 0})      (convert (fjv. 1 {"cm" 1}) {"m" 1})))
  (is (= (fjv. 100 {"m" 0})        (convert (fjv. 1 {"m" 1}) {"cm" 1})))
  (is (= (fjv. 1/50 {"m" 0})       (convert (fjv. 2 {"cm" 1}) {"m" 1})))
  (is (= (fjv. 1/254 {"m" 0})      (convert (fjv. 1 {"dm" 1}) {"kinch" 1})))

  (is (thrown? Exception           (convert (fjv. 1 {"m" 1}) {"s" 1})))

  ;; implicit inversion
  (is (= (fjv. 1/2 {"m" 0})        (convert (fjv. 2 {"m" 1}) {"m" -1})))
  (is (= (fjv. 1/3 {"m" 0 "s" 0})  (convert (fjv. 3 {"m" -1 "s" 1}) {"m" 1 "s" -1})))
  (is (thrown? Exception           (convert (fjv. 3 {"m" -1 "s" 1}) {"m" 1 "s" 1})))
  )
  
  