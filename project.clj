(defproject frinj "0.2.6-SNAPSHOT"
  :description "Practical unit-of-measure calculator DSL for Clojure"
  :url "https://github.com/martintrojer/frinj"

  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/clojurescript "0.0-2371"]]

  ;; Clojure
  :profiles {:dev {:dependencies [[org.clojure/tools.namespace "0.2.4"]
                                  [org.clojure/tools.trace "0.7.6"]]
                   :source-paths ["dev"]}}
  :repl-options {:init (user/go)}

  ;; CLJS
  :plugins [[lein-cljsbuild "0.3.2"]]
  :hooks [leiningen.cljsbuild]
  :cljsbuild {:crossovers [frinj.cross frinj.core frinj.ops frinj.parser]
              :crossover-jar true
              :builds [{:source-paths ["examples/node"]
                        :compiler {:output-to "frinj-node.js"
                                   :target :nodejs
                                   :externs ["examples/node/externs.js"]
                                   :optimizations :advanced
                                   :pretty-print false}}
                       {:source-paths ["examples/browser"]
                        :compiler {:output-to "browser-example/frinj.js"
                                   :optimizations :whitespace
                                   :pretty-print false}}]})
