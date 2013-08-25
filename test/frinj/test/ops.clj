;;  Copyright (c) Martin Trojer. All rights reserved.
;;  The use and distribution terms for this software are covered by the
;;  Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;;  which can be found in the file epl-v10.html at the root of this distribution.
;;  By using this software in any fashion, you are agreeing to be bound by
;;  the terms of this license.
;;  You must not remove this notice, or any other, from this software.

(ns frinj.test.ops
  (:use [clojure.test]
        [frinj.core :only [reset-state! state]]
        [frinj.ops])
  (:import frinj.core.fjv))

(defn- calc-test-fixture [f]
  (reset-state!)
  (swap! state assoc-in [:prefixes "d"] (fjv. 1/10 {}))
  (swap! state assoc-in [:prefixes "c"] (fjv. 1/100 {}))
  (swap! state assoc-in [:standalone-prefixes "kilo"] (fjv. 1000 {}))
  (swap! state assoc-in [:prefixes "k"] (fjv. 1000 {}))
  (swap! state assoc-in [:units "m"] (fjv. 1 {}))
  (swap! state assoc-in [:units "s"] (fjv. 1 {}))
  (swap! state assoc-in [:units "pi"] (fjv. 3.14 {}))
  (swap! state assoc-in [:units "inch"] (fjv. 127/5000 {"m" 1}))
  (swap! state assoc-in [:fundamental-units {"m" 1}] (fjv. 1 {}))
  (swap! state update-in [:fundamentals] #(conj % "m"))
  (swap! state update-in [:fundamentals] #(conj % "s"))
  (f))

(use-fixtures :once calc-test-fixture)

(deftest test-fj
  (is (= one                     (fj)))
  (is (= (fjv. 1 {"m" 1})        (fj "m")))
  (is (= (fjv. 1 {"m" 1})        (fj :m)))
  (is (= (fjv. 1/50 {"m" 1})     (fj 2 :cm)))
  (is (= (fjv. 1/5 {"m" 1})      (fj :dm 2)))

  (is (= (fjv. 3.14 {"m" 1 "s" -1})
         (fj 3.14 :m :per :s)))
  (is (= (fjv. 1/2 {"m" 1 "s" 1})
         (fj :m :per 2 :s)))
  (is (= (fjv. 1/2 {"m" 1 "s" -1})
         (fj :m :per 2 :per :s)))

  (is (= (fjv. 50/127 {})        (fj :cm :to :inch)))
  (is (= one                     (fj :cm :to :cm)))
  (is (= one                     (fj :cm :per :cm)))
  (is (thrown? Exception         (fj :cm :to)))
  (is (thrown? Exception         (fj :cm :to :to)))
  (is (thrown? Exception         (fj :cm :to 1)))

  (is (= (fjv. 3.14 {"s" -1})    (fj :pi :per :s)))
  (is (thrown? Exception         (fj :cm :to :s)))

  (is (= (fjv. 1286492400 {"s" 1})
         (fj :#2010-10-08)))

  (is (fj<= (fj :#now) (fj :#now)))

  (is (= (fjv. 1/2 {})           (fj 1 :per 2)))
  )

(deftest test-to
  (is (= one                     (to one 1)))
  (is (= (fjv. 1/2 {})           (to one 2)))
  (is (= (fjv. 50/127 {})        (to (fjv. 1 {"cm" 1}) "inch")))
  (is (= (fjv. 50/127 {})        (to (fjv. 1 {"cm" 1}) :inch)))
  (is (= (fjv. 127/500 {})       (to (fjv. 1 {"inch" 1}) 10 :cm)))
  )
