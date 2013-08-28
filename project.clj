(defproject frinj "0.2.2-SNAPSHOT"
  :description "Practical unit-of-measure calculator DSL for Clojure"
  :url "https://github.com/martintrojer/frinj"

  ;; Clojure

  :dependencies [[org.clojure/clojure "1.5.1"]]
  :source-paths ["src"]
  :profiles {:dev {:dependencies [[org.clojure/tools.namespace "0.2.4"]
                                  [org.clojure/tools.trace "0.7.6"]]
                   :source-paths ["dev"]}}
  :repl-options {:init (user/go)}

  ;; CLJS / Node.js

  :plugins [[lein-cljsbuild "0.3.2"]]
  :hooks [leiningen.cljsbuild]
  :cljsbuild {:crossovers [frinj.cross frinj.core frinj.ops frinj.parser]
              :crossover-jar true
              :builds [{:source-paths ["examples/node"]
                        :compiler {:output-to "frinj.js"
                                   :target :nodejs
                                   :externs ["examples/node/externs.js"]
                                   :optimizations :advanced
                                   :pretty-print false}}]}
  )
