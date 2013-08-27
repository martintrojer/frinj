(ns frinj.node
  (:require [frinj.core :as core]
            [cljs.reader :as reader]))

(defn -main [& args]
  (let [fs (js/require "fs")
        edn (fs/readFileSync "resources/units.edn")
        state-edn (reader/read-string edn)]
    (println state-edn)))

(set! *main-cli-fn* -main)
