;;  Copyright (c) Martin Trojer. All rights reserved.
;;  The use and distribution terms for this software are covered by the
;;  Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;;  which can be found in the file epl-v10.html at the root of this distribution.
;;  By using this software in any fashion, you are agreeing to be bound by
;;  the terms of this license.
;;  You must not remove this notice, or any other, from this software.

(ns frinj.core)

(def ^{:dynamic true} *debug* (atom false))
(defn enable-debug! [] (reset! *debug* true))

;; =================================================================
;; Core unit states, and helper functions

;; TODO; refs + dosync?
(def units (atom {}))
(def prefixes (atom {}))
(def standalone-prefixes (atom {}))
(def fundamental-units (atom {}))
(def fundamentals (atom #{}))

(defn reset-states!
  "Total reset of the core unit states"
  []
  (reset! units {})
  (reset! prefixes {})
  (reset! standalone-prefixes {})
  (reset! fundamental-units {})
  (reset! fundamentals #{})
  )

(defn to-unit-str
  "Covert to a unit string"
  [op]
  (if (keyword? op)
    (.substring (str op) 1)
    (str op)))

(defn add-with-plurals!
  "Adds a unit to the state (and it's potential plural)"
  [atm name fj]
  (let [name (to-unit-str name)]    
    (swap! atm #(assoc % name fj))
    (when-not (or (= \s (last name)) (= 1 (.length name)))
      (swap! atm #(assoc % (str name "s") fj)))
    fj))

(defn add-unit!
  "Adds a unit to the state"
  [name fj]
  (add-with-plurals! units name fj))

(defn export-states-to-file
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
    (reset! units nunits)
    (reset! prefixes nprefixes)
    (reset! standalone-prefixes nstandalone-prefixes)
    (reset! fundamental-units nfundamental-units)
    (reset! fundamentals nfundamentals)))

;; =================================================================
;; Core units of measure types and functions

(defn- clean-us
  "Remove all zero-values entries"
  [m]
  (->> m (filter (fn [[_ v]] (not= v 0))) (into {})))

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
                  "" (clean-us u))
          "[" (get @fundamental-units (clean-us u) "") "]"))))

(def one (fjv. 1 {}))
(def zero (fjv. 0 {}))

(defn add-units
  "Adds units"
  [& us]
  (loop [acc {}, us us]
    (let [m (first us)]
      (when @*debug* (println "add-units" acc m us))
      (if-not (empty? us)
        (recur       
         (reduce (fn [acc [k v]]
                   (if-let [av (get acc k)]
                     (assoc acc k (+ av v))
                     (assoc acc k v)))
                 acc (seq m))
         (rest us))
        acc))))

(defn clean-units
  "Remove 0-ed units from a fjv"
  [fj]
  (fjv. (:v fj) (clean-us (:u fj))))

(defn- to-fjs
  [fjs]
  (map #(if (= frinj.core.fjv (class %)) % (fjv. % {})) fjs))

(defn- add-sub
  [op fjs]
  (let [fjs (to-fjs fjs)]    
    (when-let [u (:u (first fjs))]
      (let [vs (map #(:v %) fjs)
            us (into #{} (map #(clean-us (:u %)) fjs))]
        (when @*debug* (println "add-sub" fjs u vs us))
        (if (= (count us) 1)
          (fjv. (reduce op vs) u)
          (throw (Exception. "Cannot add units with different dimensions")))))))

(defn fj-add
  "Adds fjvs"
  [& fjs]
  (add-sub + fjs))

(defn fj-sub
  "Subtracts fjvs"
  [& fjs]
  (add-sub - fjs))

(defn- flip-sign
  "Swaps the sign of units"
  [u]
  (->>
   (map (fn [[k v]] [k (- v)]) u)
   (reduce #(conj %1 %2) {})))

;; (flip-sign {:a 1 :b -12})
;; (flip-sign {:a -1 :b 0})

(defn fj-mul
  [& fjs]
  (let [fjs (to-fjs fjs)]    
    (when @*debug* (println "*" fjs))
    (when-not (empty? fjs)
      (let [v (reduce * (map #(:v %) fjs))
            u (reduce add-units (map #(:u %) fjs))]
        (fjv. v u)))))

(defn fj-div
  [& fjs]
  (let [fjs (to-fjs fjs)]    
    (when @*debug* (println "/" fjs))
    (when-not (empty? fjs)
      (let [v (reduce / (map #(:v %) fjs))
            first-u (:u (first fjs))
            rest-u (map #(flip-sign (:u %)) (rest fjs))
            u (reduce add-units (cons first-u rest-u))]
        (fjv. v u)))))

(defn fj-inverse
  [fj]
  (fj-div 1 fj))

(defn fj-int-pow
  "Power operator, only positive integers!"
  [fj exp]
  (if (integer? exp)
    (if (pos? exp)
      (reduce (fn [acc _] (fj-mul acc fj)) one (range exp))
      (reduce (fn [acc _] (fj-div acc fj)) one (range (- exp))))
    (throw (Exception. "only integers supported"))))

(defn fj-equal?
  [fst-fjv & fjs]
  (loop [[fst & rst] fjs]
    (if (nil? fst)
      true
      (if-not (= 0 (:v (fj-sub fst-fjv fst)))
        false
        (recur rst)))))

(defn fj-less
  [& fjs]
  (loop [[fst snd & rst] fjs]
    (if (or (nil? fst) (nil? snd))
      true
      (if-not (< 0 (:v (fj-sub snd fst)))
        false
        (recur (into rst [snd]))))))

(defn fj-greater
  [& fjs]
  (loop [[fst snd & rst] fjs]
    (if (or (nil? fst) (nil? snd))
      true
      (if-not (> 0 (:v (fj-sub snd fst)))
        false
        (recur (into rst [snd]))))))