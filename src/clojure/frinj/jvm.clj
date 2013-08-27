;;  Copyright (c) Martin Trojer. All rights reserved.
;;  The use and distribution terms for this software are covered by the
;;  Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;;  which can be found in the file epl-v10.html at the root of this distribution.
;;  By using this software in any fashion, you are agreeing to be bound by
;;  the terms of this license.
;;  You must not remove this notice, or any other, from this software.

(ns frinj.jvm
  (:require [frinj.core :as core]
            [frinj.ops :as ops]
            [frinj.parser :as parser]
            [frinj.feeds :as feeds]
            [clojure.java.io :as io])
  (:import frinj.core.fjv))

(def ^:private unit-clj-file (io/resource "units.edn"))

(defn- restore-state! []
  (-> unit-clj-file slurp read-string core/restore-state!))

(defn frinj-init!
  "Init the frinj envrionment. Will try to load the clj-unit file - if that fails the unit.txt file"
  []
  (restore-state!)
  (feeds/setup-feeds)
  :done)

(def ^:private unit-txt-file (io/resource "units.txt"))

(defn load-unit-txt-file!
  "Resets the states and loads units from the frink units.txt file"
  []
  (with-open [rdr (io/reader unit-txt-file)]
    (parser/restore-state-from-text! (line-seq rdr))))

(defn- get-seconds
  "Get number of seconds since EPOC given a yyyy-mm-dd datestring"
  [ds]
  (let [df (java.text.SimpleDateFormat. "yyyy-MM-dd")
        date (if (= (.toLowerCase ds) "now") (java.util.Date.) (.parse df ds))]
    (/ (.getTime date) 1000)))

(alter-var-root #'ops/*get-seconds* (fn [_] get-seconds))

(defn to-date
  "Convert a fj of units {s 1} to a date string"
  [fj]
  (let [fj (core/clean-units fj)]
    (if (= (:u fj) {"s" 1})
      (let [date (java.util.Date. (long  (* 1000 (:v fj))))]
        (str date))
      (throw (Exception. "cannot convert type to a date")))))

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

  (defmethod clojure.core/print-method fjv [x writer]
    (.write writer (str x))))
