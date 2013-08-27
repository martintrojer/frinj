;;  Copyright (c) Martin Trojer. All rights reserved.
;;  The use and distribution terms for this software are covered by the
;;  Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;;  which can be found in the file epl-v10.html at the root of this distribution.
;;  By using this software in any fashion, you are agreeing to be bound by
;;  the terms of this license.
;;  You must not remove this notice, or any other, from this software.

(ns frinj.jvm
  (:require [frinj.core :as core]
            [frinj.cross :as cross]
            [frinj.parser :as parser]
            [frinj.feeds :as feeds]
            [clojure.java.io :as io])
  (:import frinj.core.fjv))

(alter-var-root #'cross/starts-with (fn [_] (fn [^String s ^String prefix] (.startsWith s prefix))))
(alter-var-root #'cross/sub-string (fn [_] (fn [^String s ^String prefix] (.substring s (.length prefix)))))
(alter-var-root #'cross/ratio? (fn [_] ratio?))
(alter-var-root #'cross/throw-exception (fn [_] (fn [^String s] (throw (Exception. s)))))

(def ^:private unit-clj-file (io/resource "units.edn"))

(defn frinj-init!
  "Init the frinj envrionment. Will try to load the clj-unit file - if that fails the unit.txt file"
  []
  (-> unit-clj-file slurp read-string core/restore-state!)
  (feeds/setup-feeds)
  :done)

(def ^:private unit-txt-file (io/resource "units.txt"))

(defn load-unit-txt-file!
  "Resets the states and loads units from the frink units.txt file"
  []
  (with-open [rdr (io/reader unit-txt-file)]
    (parser/restore-state-from-text! (line-seq rdr))))

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
