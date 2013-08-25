;;  Copyright (c) Martin Trojer. All rights reserved.
;;  The use and distribution terms for this software are covered by the
;;  Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;;  which can be found in the file epl-v10.html at the root of this distribution.
;;  By using this software in any fashion, you are agreeing to be bound by
;;  the terms of this license.
;;  You must not remove this notice, or any other, from this software.

(ns frinj.ops
  (:require [frinj.core :as core]
            [frinj.parser :as parser]
            [frinj.feeds :as feeds]
            [clojure.java.io :as io])
  (:import frinj.core.fjv))

(defn frinj-init!
  "Init the frinj envrionment. Will try to load the clj-unit file - if that fails the unit.txt file"
  []
  (core/restore-state!)
  (feeds/setup-feeds)
  :done)

(def ^:private unit-txt-file (io/resource "units.txt"))

(defn load-unit-txt-file!
  "Resets the states and loads units from the frink units.txt file"
  []
  (core/reset-state!)
  (with-open [rdr (io/reader unit-txt-file)]
    (doseq [line (line-seq rdr)]
      (-> line parser/tokenize parser/parse!))))

;; =================================================================
;; fjv creation and conversion

(defn- get-seconds
  "Get number of seconds since EPOC given a yyyy-mm-dd datestring"
  [ds]
  (let [df (java.text.SimpleDateFormat. "yyyy-MM-dd")
        date (if (= (.toLowerCase ds) "now") (java.util.Date.) (.parse df ds))]
    (/ (.getTime date) 1000)))

(defn- map-fj-operators
  "Maps (fj) operators to tokens"
  [os]
  (when @core/*debug* (println "map-fj-ops" os))
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

;; =================================================================
;; Operators

(def add-unit! core/add-unit!)
(def one core/one)
(def zero core/zero)

(defn fj
  "Convenience function to build fjv's"
  ([] core/one)
  ([& os]
     (let [[toks to] (map-fj-operators os)
           [u fact _] (parser/eat-units toks)
           fj (core/resolve-and-normalize-units u)
           res (fjv. (* fact (:v fj)) (:u fj))]
       (if to
         (core/clean-units (core/convert res {to 1}))
         res))))

(defn to
  "Convert a fjv to a unit. Unit is specified with (fj) ops"
  [fj & os]
  (when @core/*debug* (println "to" fj os))
  (let [[toks _] (map-fj-operators os)
        [u fact] (parser/eat-units toks)
        cfj (core/convert fj u)
        res (fjv. (/ (:v cfj) fact) (:u cfj))]
    (core/clean-units res)))

(defn to-date
  "Convert a fj of units {s 1} to a date string"
  [fj]
  (let [fj (core/clean-units fj)]
    (if (= (:u fj) {"s" 1})
      (let [date (java.util.Date. (long  (* 1000 (:v fj))))]
        (str date))
      (throw (Exception. "cannot convert type to a date")))))

(defn find-units [s]
  (let [pat (re-pattern s)]
    (->> @core/state :units
         (filter #(re-find pat (first %))))))

(defn find-fundamentals [s]
  (let [fus (->> @core/state :fundamental-units)
        pat (re-pattern s)]
    (->> (zipmap (vals fus) (keys fus))
         (filter #(re-find pat (first %))))))

(def fj+ core/fj-add)
(def fj- core/fj-sub)
(def fj* core/fj-mul)
(def fj_ core/fj-div)           ;; can't find a better character for div!
(def fj** core/fj-int-pow)
(def fj= core/fj-equal?)
(def fj< core/fj-less?)
(def fj> core/fj-greater?)
(def fj<= core/fj-less-or-equal?)
(def fj>= core/fj-greater-or-equal?)

(defn override-operators! []
  (eval '(do
           (def + fj+)
           (def - fj-)
           (def * fj*)
           (def / fj_)
           (def < fj<)
           (def > fj>)
           (def <= fj<=)
           (def >= fj>=)))

  (defmethod clojure.core/print-method frinj.core.fjv [x writer]
    (.write writer (str x))))
