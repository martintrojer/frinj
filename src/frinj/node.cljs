;;  Copyright (c) Martin Trojer. All rights reserved.
;;  The use and distribution terms for this software are covered by the
;;  Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;;  which can be found in the file epl-v10.html at the root of this distribution.
;;  By using this software in any fashion, you are agreeing to be bound by
;;  the terms of this license.
;;  You must not remove this notice, or any other, from this software.

(ns frinj.node
  (:require [frinj.core :as core]
            [cljs.reader :as reader]))

;; NodeJS specific functions

(defn frinj-init! [filename]
  (let [fs (js/require "fs")
        edn (fs/readFileSync "resources/units.edn")
        state-edn (reader/read-string (str edn))]
    (core/restore-state! state-edn)))
