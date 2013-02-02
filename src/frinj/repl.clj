;;  Copyright (c) Martin Trojer. All rights reserved.
;;  The use and distribution terms for this software are covered by the
;;  Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;;  which can be found in the file epl-v10.html at the root of this distribution.
;;  By using this software in any fashion, you are agreeing to be bound by
;;  the terms of this license.
;;  You must not remove this notice, or any other, from this software.

(ns frinj.repl
  (:require [frinj.infix])
  (:use [frinj.feeds]))

(defn immigrate
 "Create a public var in this namespace for each public var in the
 namespaces named by ns-names. The created vars have the same name, value
 and metadata as the original except that their :ns metadata value is this
 namespace."
 [& ns-names]
 (doseq [ns ns-names]
   (doseq [[sym var] (ns-publics ns)]
     (let [sym (with-meta sym (assoc (meta var) :orig-ns ns))]
       (if (.isBound var)
         (intern *ns* sym (var-get var))
         (intern *ns* sym))))))

(immigrate 'frinj.core 'frinj.calc 'frinj.infix)

(defn frinj-reset! []
  (frinj-init!)
  (restart-exchange-feed!)
  (restart-precious-metal-feed!)
  (restart-industrial-metal-feed!)
  (restart-agrarian-feed!))

(frinj-reset!)

(println "Welcome to Frinj!")

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
  )
