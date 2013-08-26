(defproject frinj "0.2.2-SNAPSHOT"
  :description "Practical unit-of-measure calculator DSL for Clojure"
  :url "https://github.com/martintrojer/frinj"

  ;; Clojure

  :dependencies [[org.clojure/clojure "1.5.1"]]
  :source-paths ["src/clojure"]
  :profiles {:dev {:dependencies [[org.clojure/tools.namespace "0.2.4"]
                                  [org.clojure/tools.trace "0.7.6"]]
                   :source-paths ["dev"]}}
  :repl-options {:init (user/go)}

  ;; CLJS / Node.js

  :plugins [[lein-cljsbuild "0.3.2"]]
  :cljsbuild {:crossovers [frinj.core frinj.ops frinj.parser]
              :builds [{:source-paths ["target/cljsbuild-crossover/frinj" "src/cljs/frinj"]
                        :compiler {:output-to "frinj.js"
                                   :target :nodejs
                                   :optimizations :simple
                                   :pretty-print true}}]}
  )
