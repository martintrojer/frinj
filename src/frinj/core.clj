;;  Copyright (c) Martin Trojer. All rights reserved.
;;  The use and distribution terms for this software are covered by the
;;  Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;;  which can be found in the file epl-v10.html at the root of this distribution.
;;  By using this software in any fashion, you are agreeing to be bound by
;;  the terms of this license.
;;  You must not remove this notice, or any other, from this software.

(ns frinj.core)

(def ^:dynamic *debug* (atom false))
(defn enable-debug! [] (reset! *debug* true))

;; =================================================================
;; Core unit states, and helper functions

(def units (ref {}))
(def prefixes (ref {}))
(def standalone-prefixes (ref {}))
(def fundamental-units (ref {}))
(def fundamentals (ref #{}))

(defn reset-states!
  "Total reset of the core unit states (to empty)"
  []
  (dosync
   (ref-set units {})
   (ref-set prefixes {})
   (ref-set standalone-prefixes {})
   (ref-set fundamental-units {})
   (ref-set fundamentals #{})))

(defn add-with-plurals!
  "Adds a unit to the state (and it's potential plural)"
  [rf uname fj]
  (let [uname (name uname)]
    (dosync
     (alter rf #(assoc % uname fj))
     (when-not (or (= \s (last uname)) (= 1 (.length uname)))
       (alter rf #(assoc % (str uname "s") fj)))
     fj)))

(defn add-unit!
  "Adds a unit to the state"
  [name fj]
  (add-with-plurals! units name fj))

(defn export-states-to-string
  "Exports the state in clojure format to a string"
  []
  (pr-str {:units @units
           :prefixes @prefixes
           :standalone-prefixes @standalone-prefixes
           :fundamental-units @fundamental-units
           :fundamentals @fundamentals}))

(defn import-states!
  "Import states from a clojure-formatted file"
  [data]
  (let [{nunits :units
         nprefixes :prefixes
         nstandalone-prefixes :standalone-prefixes
         nfundamental-units :fundamental-units
         nfundamentals :fundamentals} (read-string data)]
    (dosync
     (ref-set units nunits)
     (ref-set prefixes nprefixes)
     (ref-set standalone-prefixes nstandalone-prefixes)
     (ref-set fundamental-units nfundamental-units)
     (ref-set fundamentals nfundamentals))))

;; =================================================================
;; Core units of measure types and functions

(defn- clean-us
  "Remove all zero-values entries"
  [m]
  (->> m
       (remove (fn [[_ v]] (zero? v)))
       (into {})))

;; fjv is the core type representing values w/ UOM.
;; :v is the value
;; :u is the unit, represented by a map.
;;    keys are the dimentsions, and the value is the magnitude

(defrecord fjv [v u]
  Object
  (toString [this]
    (str (if (ratio? v) (str v " (approx. " (double v) ")") v) " "
         (str
          (reduce (fn [acc [k v]] (str acc (if (= v 1) (str k " ") (str k "^" v " "))))
                  "" (->> u clean-us (into (sorted-map))))
          "[" (get @fundamental-units (clean-us u) "") "]"))))

(def one (fjv. 1 {}))
(def zero (fjv. 0 {}))

(defn add-units [& us] (apply merge-with (fnil + 0) us))

(defn clean-units
  "Remove 0-ed units from a fjv"
  [fj]
  (fjv. (:v fj) (clean-us (:u fj))))

(defn- to-fjs
  "create fjvs from numbers"
  [nums]
  (map #(if (= frinj.core.fjv (class %)) % (fjv. % {})) nums))

(defn- enfore-units
  "Enforce all fjs' units are the same"
  [fjs]
  (when (> (->> fjs (map :u) (map clean-us) set count) 1)
    (throw (Exception. "Cannot use operator on units with different dimensions"))))

(defn- add-sub
  [op fjs]
  (when-let [fjs (-> fjs to-fjs seq)]
    (when @*debug* (println "add-sub" fjs (map :v fjs) (map :u fjs)))
    (fjv. (reduce op (map :v fjs)) (-> fjs first :u))))

(defn fj-add
  "Adds fjvs"
  [& fjs]
  (enfore-units fjs)
  (add-sub + fjs))

(defn fj-sub
  "Subtracts fjvs"
  [& fjs]
  (enfore-units fjs)
  (add-sub - fjs))

(defn- flip-sign
  "Swaps the sign of units"
  [us]
  (->> us
       (map (fn [[k v]] [k (- v)]))
       (into {})))

;; (flip-sign {:a 1 :b -12})
;; (flip-sign {:a -1 :b 0})

(defn fj-mul
  [& fjs]
  (when-let [fjs (-> fjs to-fjs seq)]
    (when @*debug* (println "*" fjs))
    (fjv. (reduce * (map :v fjs))
          (apply add-units (map :u fjs)))))

(defn fj-div
  [& fjs]
  (when-let [fjs (-> fjs to-fjs seq)]
    (when @*debug* (println "/" fjs))
    (fjv. (reduce / (map :v fjs))
          (apply add-units (concat [(-> fjs first :u)]
                                   (->> fjs rest (map :u) (map flip-sign)))))))

(defn fj-inverse
  [fj]
  (fj-div 1 fj))

(defn fj-int-pow
  "Power operator, only integers!"
  [fj exp]
  (if (integer? exp)
    (if (pos? exp)
      (reduce (fn [acc _] (fj-mul acc fj)) one (range exp))
      (reduce (fn [acc _] (fj-div acc fj)) one (range (- exp))))
    (throw (Exception. "only integers supported"))))

(defn fj-equal?
  [& fjs]
  (enfore-units fjs)
  (->> fjs (map clean-units) (apply =)))

(defn fj-not-equal?
  [& fjs]
  (enfore-units fjs)
  (not (apply fj-equal? fjs)))

(defn fj-less?
  [& fjs]
  (enfore-units fjs)
  (->> fjs (map :v) (apply <)))

(defn fj-greater?
  [& fjs]
  (enfore-units fjs)
  (->> fjs (map :v) (apply >)))

(defn fj-less-or-equal? [& fjs]
  (enfore-units fjs)
  (or (apply fj-equal? fjs) (apply fj-less? fjs)))

(defn fj-greater-or-equal? [& fjs]
  (enfore-units fjs)
  (or (apply fj-equal? fjs) (apply fj-greater? fjs)))
