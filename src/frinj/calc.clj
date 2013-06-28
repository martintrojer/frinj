;;  Copyright (c) Martin Trojer. All rights reserved.
;;  The use and distribution terms for this software are covered by the
;;  Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;;  which can be found in the file epl-v10.html at the root of this distribution.
;;  By using this software in any fashion, you are agreeing to be bound by
;;  the terms of this license.
;;  You must not remove this notice, or any other, from this software.

(ns frinj.calc
  (:use [frinj.core]
        [frinj.utils]
        [frinj.parser])
  (:import [clojure.java.io]
           [frinj.core fjv]))

;; =================================================================

(defn- get-seconds
  "Get number of seconds since EPOC given a yyyy-mm-dd datestring"
  [ds]
  (let [df (java.text.SimpleDateFormat. "yyyy-MM-dd")
        date (if (= (.toLowerCase ds) "now") (java.util.Date.) (.parse df ds))]
    (/ (.getTime date) 1000)))

(defn- map-fj-operators
  "Maps (fj) operators to tokens"
  [os]
  (when @*debug* (println "map-fj-ops" os))
  (loop [acc [], to nil, [fst snd & rst] os]
    (let [r (into rst [snd])]
      (if-not (nil? fst)
        (cond
          (number? fst) (recur (conj acc [:number fst]) to r)
          (= :per fst) (recur (conj acc [:divide]) to r)
          (= :to fst) (if (and (not (nil? snd)) (or (keyword? snd) (string? snd)))
                        (let [tos (name snd)]
                          (if-not (= tos "to")
                            (recur acc tos rst)
                            (throw (Exception. "invalid to target"))))
                        (throw (Exception. "invalid to target")))
          (string? fst) (recur (conj acc [:unit fst]) to r)
          (keyword? fst) (let [name (name fst)]
                           (if (.startsWith name "#")
                             (recur (into acc [[:number (get-seconds (.substring name 1))]
                                               [:unit "s"]])
                                    to r)
                             (recur (conj acc [:unit name]) to r)))
          :else (throw (Exception. (str "unsupported operator " fst))))
        [acc to]))))

(defn fj
  "Convenience function to build fjv's"
  ([] one)
  ([& os]
     (let [[toks to] (map-fj-operators os)
           [u fact _] (eat-units toks)
           fj (resolve-and-normalize-units u)
           res (fjv. (* fact (:v fj)) (:u fj))]
       (if to
         (clean-units (convert res {to 1}))
         res))))

(defn to
  "Convert a fjv to a unit. Unit is specified with (fj) ops"
  [fj & os]
  (when @*debug* (println "to" fj os))
  (let [[toks _] (map-fj-operators os)
        [u fact] (eat-units toks)
        cfj (convert fj u)
        res (fjv. (/ (:v cfj) fact) (:u cfj))]
    (clean-units res)))

(defn to-date
  "Convert a fj of units {s 1} to a date string"
  [fj]
  (let [fj (clean-units fj)]
    (if (= (:u fj) {"s" 1})
      (let [date (java.util.Date. (long  (* 1000 (:v fj))))]
        (str date))
      (throw (Exception. "cannot convert type to a date")))))

;; =================================================================

(def unit-clj-file (clojure.java.io/resource "units.clj"))
(def unit-txt-file (clojure.java.io/resource "units.txt"))

(defn load-unit-txt-file!
  "Resets the states and loads units from the frink units.txt file"
  []
  (reset-states!)
  (with-open [rdr (clojure.java.io/reader unit-txt-file)]
    (doseq [line (line-seq rdr)]
      (-> line (tokenize) parse!))))

;; =================================================================

(defn frinj-init!
  "Init the frinj envrionment. Will try to load the clj-unit file - if that fails the unit.txt file"
  []
  (try
    (import-states! (slurp unit-clj-file))
    (catch Exception e
      (load-unit-txt-file!))))

;; =================================================================
;; Convenient operator names

(def fj+ fj-add)
(def fj- fj-sub)
(def fj* fj-mul)
(def fj_ fj-div)           ;; can't find a better character for div!
(def fj** fj-int-pow)
(def fj= fj-equal?)
(def fj< fj-less?)
(def fj> fj-greater?)
(def fj<= fj-less-or-equal?)
(def fj>= fj-greater-or-equal?)
