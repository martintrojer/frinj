(ns frinj.repl
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

(immigrate 'frinj.core 'frinj.calc)

(defn frinj-reset! []
  (frinj-init!)
  (restart-exchange-feed!)
  (restart-precious-metal-feed!)
  (restart-industrial-metal-feed!)
  (restart-agrarian-feed!))

(frinj-reset!)


