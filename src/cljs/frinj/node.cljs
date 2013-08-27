(ns frinj.node
  (:require [frinj.core :as core]
            [frinj.ops :as ops]
            [cljs.reader :as reader]))

(defn frinj-init! []
  (let [fs (js/require "fs")
        edn (fs/readFileSync "resources/units.edn")
        state-edn (reader/read-string (str edn))]
    (core/restore-state! state-edn)))

(defn -main [& args]
  (frinj-init!)
  (println (map (fn [[k vs]] [k (count vs)]) @core/state))
  (println (ops/fj :inch))
  (println (ops/fj :cm))
  (println (-> (ops/fj :teaspoon :water :c :c) (ops/to :gallons :gasoline) str))
  )

(set! *main-cli-fn* -main)
