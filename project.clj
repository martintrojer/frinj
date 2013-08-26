(defproject frinj "0.2.2-SNAPSHOT"
  :description "Practical unit-of-measure calculator DSL for Clojure"
  :url "https://github.com/martintrojer/frinj"
  :dependencies [[org.clojure/clojure "1.5.1"]]
  :profiles {:dev {:dependencies [[org.clojure/tools.namespace "0.2.4"]
                                  [org.clojure/tools.trace "0.7.6"]]
                   :source-paths ["dev"]}}
  :repl-options {:init (user/go)}
  )
