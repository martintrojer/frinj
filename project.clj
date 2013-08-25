(defproject frinj "0.2.0"
  :description "Practical unit-of-measure calculator DSL for Clojure"
  :url "https://github.com/martintrojer/frinj"
  :dependencies [[org.clojure/clojure "1.5.1"]]
  :profiles {:dev {:dependencies [[org.clojure/tools.namespace "0.2.4"]]
                   :source-paths ["dev"]}}
  :repl-options {:init (user/go)}
  )
