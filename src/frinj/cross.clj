;;  Copyright (c) Martin Trojer. All rights reserved.
;;  The use and distribution terms for this software are covered by the
;;  Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;;  which can be found in the file epl-v10.html at the root of this distribution.
;;  By using this software in any fashion, you are agreeing to be bound by
;;  the terms of this license.
;;  You must not remove this notice, or any other, from this software.

(ns frinj.cross
  (:refer-clojure :exclude [ratio?]))

;; Some functions to abstract JVM / NodeJS platform differences.
;; Implementations here are "CLJS safe".

;; --- overwritten by jvm.clj

(defn starts-with [s prefix]
  (let [l (.-length prefix)]
    (= (apply str (take l s)) prefix)))

(defn sub-string [s pfx]
  (apply str (drop (.-length pfx) s)))

(defn ratio? [n] false)

(defn throw-exception [s]
  (throw s))
