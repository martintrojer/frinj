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
