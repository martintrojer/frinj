;;  Copyright (c) Martin Trojer. All rights reserved.
;;  The use and distribution terms for this software are covered by the
;;  Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;;  which can be found in the file epl-v10.html at the root of this distribution.
;;  By using this software in any fashion, you are agreeing to be bound by
;;  the terms of this license.
;;  You must not remove this notice, or any other, from this software.

(ns frinj.utils
  (:use [frinj.core])
  (:import [frinj.core fjv]))

;; =================================================================
;; prefix queries and transforms

(defn prefix?
  "It this a prefix?"
  [p]
  (or (contains? @standalone-prefixes p)
      (contains? @prefixes p)))

(defn all-prefix-names
  "Get all list of all prefix names"
  []
  (concat (keys @standalone-prefixes) (keys @prefixes)))

(defn lookup-prefix
  "Get the value of the given prefix"
  [p]
  (if-let [r (get @standalone-prefixes p)]
    r
    (get @prefixes p)))

(defn resolve-prefixed-unit
   "Finds the longest prefix in a unit name and replaces it with with factor"
   [uname]
   (when @*debug* (println "resolve" uname))
   (if-let [fj (get @units uname)]
     [one uname]     ;; if name = unit, just return the unit
     (if-let [pfx (->> (filter #(.startsWith uname %) (all-prefix-names))
                       (sort-by #(.length %))
                       reverse
                       (filter #(contains? @units (.substring uname (.length %))))
                       first)]
       [(lookup-prefix pfx) (.substring uname (.length pfx))]
       ;; no match, return the uname
       [one uname])))

;; {prefix:u1 1, prefix:u2 -1} -> (fj-val. fact {u1:1, u2:-1})
(defn resolve-unit-prefixes
  "Replaces all units with prefix + new unit"
  [u]
  (when @*debug* (println "resolve-units" u))
  (reduce (fn [acc [k v]]
            (let [[fact u] (resolve-prefixed-unit k)]
              (if (pos? v)
                (fj-mul (fjv. (:v acc) (add-units (:u acc) {u v}))
                        (fj-int-pow fact v))
                (fj-div (fjv. (:v acc) (add-units (:u acc) {u v}))
                        (fj-int-pow fact (Math/abs v))))))
          one u))

;; =================================================================
;; unit normaliztion

;; (fj-val. fact {u1:1, u2:-1} -> (fl-val. nfact {u0:1, u2:-1}
(defn normalize-units
  "Replaces units with already defined ones, and remove zero units"
  [fj]
  (when @*debug* (println "norm" fj))
  (->
   (reduce (fn [acc [k v]]
             (let [fj (get @units k)
                   fj (if fj fj (get @standalone-prefixes k))]
               (if fj
                 (if (contains? @fundamentals k)    ;;(= fj one)
                   acc
                   (fj-div
                    (if (pos? v)
                      (fj-mul acc (fj-int-pow fj v))
                      (fj-div acc (fj-int-pow fj (Math/abs v))))
                    (fjv. 1 {k v})))
                 acc)))
           fj (:u fj))
   (clean-units)))

(defn resolve-and-normalize-units
  "Resolve all units with prefixes, and normalized the result"
  [u]
  (-> u resolve-unit-prefixes normalize-units))

;; =================================================================
;; unit conversion

(defn convert
  "Converts a fjv to a given unit, will resolve and normalize. Will reverse if units 'mirrored'"
  [fj u]
  (when @*debug* (println "convert" fj u))
  (let [nf (resolve-and-normalize-units (:u fj))
        nfj (fjv. (* (:v nf) (:v fj)) (:u nf))
        nu (resolve-and-normalize-units u)]
    (when @*debug* (println " convert post-norm" nfj nu))
    (if (= (:u nfj) (:u nu))
      (fj-div nfj nu)
      (if (= (:u (fj-inverse nfj)) (:u nu))
        (fj-div (fj-inverse nfj) nu)
        (throw (Exception. "cannot convert to a different unit"))))))
