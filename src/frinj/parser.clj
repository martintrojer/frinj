;;  Copyright (c) Martin Trojer. All rights reserved.
;;  The use and distribution terms for this software are covered by the
;;  Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;;  which can be found in the file epl-v10.html at the root of this distribution.
;;  By using this software in any fashion, you are agreeing to be bound by
;;  the terms of this license.
;;  You must not remove this notice, or any other, from this software.

(ns frinj.parser
  (:require [frinj.core :as core]
            [frinj.cross :as cross])
  (:import frinj.core.fjv))

(def ^{:dynamic true} *trace* (atom false))  ;; trace parse results
(defn enable-trace! [] (reset! *trace* true))

;; =================================================================
;; tokenizer

;; operators
;;   ::-  prefix to unit and stand-by-itelsef value
;;   :-   prefix to unit (can't stand by itself)
;;   :=   assignment
;;   =!=  fundamental type (base dimension)
;;   |||  human readable name

(defn tokenize
  "Convert a string to a vector of (tagged-list) tokens"
  [data]
  (loop [res [], st :name, acc "", [fst snd thrd & rst] data]
    (let [r (into rst [thrd snd])
          r2 (into rst [thrd])
          append-acc (fn [] (if (empty? acc)
                             res
                             (let [acc (if (= (first acc) \.) (str "0" acc) acc)
                                   r (try                   ;; handle frinks "1ee12" form
                                       (read-string acc)
                                       (catch Exception e
                                         (try
                                           (read-string (.replaceFirst acc "ee" "e"))
                                           (catch Exception e acc))))]
                               (if (number? r)
                                 (conj res [:number r])
                                 (if (= st :name)
                                   (conj res [:name acc])
                                   (conj res [:unit acc]))))))]
      (cond
        (nil? fst) (append-acc)
        ;; "=!="
        (and (= fst \=) (= snd \!) (= thrd \=)) (recur
                                                 (conj (append-acc)
                                                        [:fundamental])
                                                 :unit "" rst)
        ;; "|||"
        (and (= fst \|) (= snd \|) (= thrd \|)) (recur
                                                 (conj (append-acc)
                                                        [:unit-combination])
                                                 :unit "" rst)
        ;; "::-"
        (and (= fst \:) (= snd \:) (= thrd \-)) (recur
                                                 (conj (append-acc)
                                                        [:standalone-prefix])
                                                 :unit "" rst)
        ;; ":-"
        (and (= fst \:) (= snd \-)) (recur (conj (append-acc) [:prefix])
                                           :unit "" r2)
        ;; ":="
        (and (= fst \:) (= snd \=)) (recur (conj (append-acc) [:assign])
                                           :unit "" r2)

        ;; "//"
        (and (= fst \/) (= snd \/)) (let [c (take-while #(not= % \newline) r2)
                                          rst (drop (inc (count c)) r2)]
                                      (recur
                                       (conj (append-acc) [:comment (.trim (apply str c))])
                                       ; (append-acc)  ; drop all comments
                                       :name "" rst))

        (= fst \() (recur (conj (append-acc) [:open]) st "" r)
        (= fst \)) (recur (conj (append-acc) [:close]) st "" r)
        (= fst \[) (recur (conj (append-acc) [:open-bracket]) st "" r)
        (= fst \]) (recur (conj (append-acc) [:close-bracket]) st "" r)
        (= fst \+) (recur (conj (append-acc) [:plus]) st "" r)
        ;; (= fst \-) (recur (conj (append-acc) [:minus]) st "" r)
        (= fst \*) (recur (conj (append-acc) [:multiply]) st "" r)
        (= fst \/) (recur (conj (append-acc) [:divide]) st "" r)
        (= fst \^) (recur (conj (append-acc) [:exp]) st "" r)
        (= fst \newline) (recur (append-acc) :name "" r)
        (Character/isWhitespace fst) (recur (append-acc) st "" r)
        :else (recur res st (str acc fst) r)))))

;; (time (def a (tokenize (slurp "units.txt"))))
;; (count a)

;; =================================================================
;; parser

(defn eat-number
  "Parse a set of tokens extracing a number in finite set of formats (as seen in frink's unit.txt file"
  [[[t1 v1] [t2 v2 :as snd] [t3 v3 :as thrd] [t4 v4 :as foth] [t5 _ :as ffth] & rst]]
  (cond
    ;; (num / num)
    (and (= t1 :open) (= t2 :number) (= t3 :divide) (= t4 :number) (= t5 :close))
    [(/ v2 v4) rst]
    ;; num / num
    (and (= t1 :number) (= t2 :divide) (= t3 :number))
    [(/ v1 v3) (into rst [ffth foth])]
    ;; num ^ num
    (and (= t1 :number) (= t2 :exp) (= t3 :number))
    [(Math/pow v1 v3) (into rst [ffth foth])]
    ;; num
    (= t1 :number)
    [v1 (into rst [ffth foth thrd snd])]
    :else (cross/throw-exception "parse error, number expected")))

(defn eat-units
  "Parse a set of tokens accumulating factors and units, frink style syntax with impied muls"
  [toks]
  (loop [acc {}, acc-fact 1, s :n, in-par false,
         [[t1 v1] [t2 _ :as snd] & rst :as toks] toks]

    (when @core/*debug* (println "eat-units" acc acc-fact s in-par toks))
    (let [r (into rst [snd])
          old (get acc v1)
          old (if (nil? old) 0 old)]
      (cond
        ;; u^number
        (and (= t1 :unit) (= t2 :exp)) (let [[n rst] (eat-number rst)]
                                         (recur
                                          (assoc acc v1
                                                 (if (= s :n)
                                                   (+ old n)
                                                   (- old n)))
                                          acc-fact s in-par rst))
        ;; u
        (= t1 :unit) (recur (assoc acc v1 (if (= s :n)
                                            (+ old 1)
                                            (- old 1)))
                            acc-fact (if in-par s :n) in-par r)
        ;; switch fron nom to demon
        (= t1 :divide) (recur acc acc-fact :d in-par r)
        ;; this is hacky, nested pars will not work
        (= t1 :open) (recur acc acc-fact true true r)
        (= t1 :close) (recur acc acc-fact false false r)
        ;; mul is implied
        (= t1 :multiply) (recur acc acc-fact s in-par r)
        ;; skip comments
        (= t1 :comment) (recur acc acc-fact s in-par r)
        (= t1 :number) (recur acc
                              (if (= s :n)
                                (* acc-fact v1)
                                (/ acc-fact v1))
                              (if in-par s :n) in-par r)
        (= t1 :plus) (cross/throw-exception "unexpected operator")
        :else [acc acc-fact toks]))))

;; the parser mutates the state directly, this is because of the nature of
;; the frink configuration file where dependencies between prefixes and units
;; are incrementally built up

(defn parse!
  "Parses a list of tokens, mutates units/fundamenta/prefix state directly"
  [toks]
  (letfn [
          (eat-prefix [[[t v :as fst] & rst]]
            (when @core/*debug* (println "eat-prefix" fst))
            (cond
              (= t :number)
              (let [[n rst] (eat-number (into rst [fst]))]
                  (if (= (ffirst rst) :unit)
                    ;; name ::- number unit+
                    (let [[u fact rst] (eat-units rst)]
                      (when @core/*debug* (println "eat-pfx" u fact))
                      [(fjv. (* fact n) u) rst])
                    ;; name ::- number
                    [(fjv. n {}) rst]))
              (= t :unit)
              (if (core/prefix? v)
                [(core/lookup-prefix v) rst]
                (cross/throw-exception "trying to assign to unknown prefix"))))

          (do-parse [acc [[t1 v1 :as fst] [t2 v2 :as snd] [t3 v3 :as thrd] & rst :as toks]]
            ;; (println "parse" toks)
            (let [r (into rst [thrd snd])]
              (cond
                (nil? t1) true

                ;; prefix definitions
                (and (= t1 :name) (= t2 :standalone-prefix))
                (let [[fj rst] (eat-prefix (into rst [thrd]))
                      rfj (core/resolve-and-normalize-units (:u fj))
                      nfj (fjv. (* (:v fj) (:v rfj)) (:u rfj))]
                  (when @*trace* (println v1 "::-" nfj))
                  (core/add-with-plurals! :standalone-prefixes v1 nfj)
                  (recur [] rst))
                (and (= t1 :name) (= t2 :prefix))
                (let [[fj rst] (eat-prefix (into rst [thrd]))
                      rfj (core/resolve-and-normalize-units (:u fj))
                      nfj (fjv. (* (:v fj) (:v rfj)) (:u rfj))]
                  (when @*trace* (println v1 ":-" nfj))
                  (swap! core/state assoc-in [:prefixes v1] nfj)
                  (recur [] rst))

                ;; fundamentals
                (and (= t1 :name) (= t2 :fundamental) (= t3 :unit))
                (let [u {v3 1}]
                  (swap! core/state assoc-in [:fundamental-units u] v1)
                  (swap! core/state update-in [:fundamentals] #(conj % v3))
                  (swap! core/state assoc-in [:units v3] core/one)
                  (when @*trace* (println v1 "=!=" u))
                  (recur [] rst))

                ;; unit combinations
                (and (= t1 :unit-combination) (= t2 :unit) (not (empty? acc)))
                (let [acc (map (fn [[t v]] [(if (= t :name) :unit t) v]) acc)
                      [u _ _] (eat-units acc)
                      rv (core/normalize-units (fjv. 1 u))]
                  (swap! core/state assoc-in [:fundamental-units (:u rv)] v2)
                  (when @*trace* (println v2 "|||" (:u rv)))
                  (recur [] (into rst [thrd])))

                ;; unit definition
                ;; name := number unit+
                (and (= t1 :name) (= t2 :assign) (= t3 :number))
                (let [[n rst] (eat-number (into rst [thrd]))
                      [u fact rst] (eat-units rst)
                      fj (core/resolve-and-normalize-units u)
                      nfact (* fact n (:v fj))
                      nu (:u fj)]
                  (core/add-unit! v1 (fjv. nfact nu))
                  (when @*trace* (println v1 ":=" nfact nu))
                  (recur [] rst))
                ;; name := unit+
                (and (= t1 :name) (= t2 :assign) (= t3 :unit))
                (let [[u fact rst] (eat-units (into rst [thrd]))
                      fj (core/resolve-and-normalize-units u)
                      nfact (* fact (:v fj))
                      nu (:u fj)]
                  (core/add-unit! v1 (fjv. nfact nu))
                  (when @*trace* (println v1 ":=" nfact nu ))
                  (recur [] rst))

                :else (recur (conj acc fst) r))))]

    (do-parse [] toks)))

(defn restore-state-from-text!
  "Restores state by parsing a sequence of Frink lines"
  [lines]
  (core/reset-state!)
  (doseq [line lines]
    (-> line tokenize parse!)))
