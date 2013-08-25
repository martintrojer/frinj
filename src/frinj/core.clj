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
;; Our state

(def state (atom nil))

(defn reset-state!
  "Total reset of the core unit states (to empty)"
  []
  (reset! state
          {:units {}
           :prefixes {}
           :standalone-prefixes {}
           :fundamental-units {}
           :fundamentals #{}}))

;; ------------------------
;; manipulate the state

(defn add-with-plurals!
  "Adds a unit to the state (and it's potential plural)"
  [k uname fj]
  (let [uname (name uname)]
    (swap! state assoc-in [k uname] fj)
    (when-not (or (= \s (last uname)) (= 1 (.length uname)))
      (swap! state assoc-in [k (str uname "s")] fj))
    fj))

(defn add-unit!
  "Adds a unit to the state"
  [name fj]
  (add-with-plurals! :units name fj))

;; ------------------------
;; queries

(defn prefix?
  "It this a prefix?"
  [p]
  (or (get-in @state [:standalone-prefixes p])
      (get-in @state [:prefixes p])))

(defn all-prefix-names
  "Get all list of all prefix names"
  []
  (concat (-> @state :standalone-prefixes keys)
          (-> @state :prefixes keys)))

(defn lookup-prefix
  "Get the value of the given prefix"
  [p]
  (get-in @state [:standalone-prefixes p]
          (get-in @state [:prefixes p])))

;; =================================================================
;; The core "Number" class

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
          "[" (get (:fundamental-units @state) (clean-us u) "") "]"))))

(def one (fjv. 1 {}))
(def zero (fjv. 0 {}))

;; =================================================================
;; Helpers

(defn clean-units
  "Remove 0-ed units from a fjv"
  [fj]
  (fjv. (:v fj) (clean-us (:u fj))))

(defn- to-fjs
  "create fjvs from numbers"
  [nums]
  (map #(if (= fjv (type %)) % (fjv. % {})) nums))

(defn- enfore-units
  "Enforce all fjs' units are the same"
  [fjs]
  (when (> (->> fjs (map :u) (map clean-us) set count) 1)
    (throw (Exception. "Cannot use operator on units with different dimensions"))))

;; =================================================================
;; Primitve math functions

(defn add-units [& us] (apply merge-with (fnil + 0) us))

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

;; =================================================================
;; prefix transforms

(defn resolve-prefixed-unit
   "Finds the longest prefix in a unit name and replaces it with with factor"
   [uname]
   (when @*debug* (println "resolve" uname))
   (if-let [fj (get-in @state [:units uname])]
     [one uname]     ;; if name = unit, just return the unit
     (if-let [pfx (->> (filter #(.startsWith uname %) (all-prefix-names))
                       (sort-by #(.length %))
                       reverse
                       (filter #(contains? (:units @state) (.substring uname (.length %))))
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
             (let [fj (get-in @state [:units k])
                   fj (if fj fj (get-in @state [:standalone-prefixes k]))]
               (if fj
                 (if (get-in @state [:fundamentals k]) ;;(= fj one)
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
