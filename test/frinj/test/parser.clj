;;  Copyright (c) Martin Trojer. All rights reserved.
;;  The use and distribution terms for this software are covered by the
;;  Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;;  which can be found in the file epl-v10.html at the root of this distribution.
;;  By using this software in any fashion, you are agreeing to be bound by
;;  the terms of this license.
;;  You must not remove this notice, or any other, from this software.

(ns frinj.test.parser
  (:use [clojure.test]
        [frinj.core :only [state reset-state! lookup-prefix]]
        [frinj.ops]
        [frinj.parser]
        [frinj.jvm])
  (:import frinj.core.fjv))

(defn- parser-test-fixture [f]
  (reset-state!)

  (print "parsing...")
  (flush)
  (let [start (System/currentTimeMillis)]
    (load-unit-txt-file!)
    (println (- (System/currentTimeMillis) start) "ms"))
  (f))

(use-fixtures :once parser-test-fixture)

(deftest parsed-units
  (is (= (fjv. 0.06032246 {"m" 2})       (get-in @state [:units "lettersize"])))
  (is (= (fjv. 0.06032246 {"m" 2})       (get-in @state [:units "lettersizes"])))
  (is (= (fjv. 1.2566370614359173E-6 {"A" -2, "s" -2, "m" 1, "kg" 1})
         (get-in @state [:units "mu0"])))
  (is (= (fjv. 1.0545717253362894E-34 {"kg" 1 "m" 2 "s" -1})
         (get-in @state [:units "hbar"])))
  (is (= (fjv. 127/5000 {"m" 1})         (get-in @state [:units "inch"])))
  (is (= (fjv. 381/1250 {"m" 1})         (get-in @state [:units "foot"])))
  (is (= (fjv. 5.067074790974977E-10 {"m" 2})
         (get-in @state [:units "circularmil"])))
  (is (= (fjv. 1.0e-26 {"kg" 1 "s" -2})  (get-in @state [:units "fluxunit"])))
  (is (= (fjv. 498951607/4800000000 {"kg" 1})
         (get-in @state [:units "liang"])))
  (is (= one                             (get-in @state [:units "steradian"])))
  (is (= one                             (get-in @state [:units "sr"])))
  (is (= (fjv. 99/200 {"m" 1})           (get-in @state [:units "sumeriancubit"])))
  (is (= (fjv. 1 {"m" -1})               (get-in @state [:units "diopter"])))
  (is (= (fjv. 1.0594630943592953 {})    (get-in @state [:units "semitone"])))
  (is (= (fjv. 2728034111/10000000000 {"kg" 1})
         (get-in @state [:units "romanaspound"])))
  (is (= (fjv. 45359237/9290304000 {"m" -2 "kg" 1})
         (get-in @state [:units "poundboxboard"])))
  (is (= (fjv. 9.27400967298574E-24 {"m" 2, "A" 1})
         (get-in @state [:units "bohrmagneton"])))
  (is (= (fjv. 10967760 {"m" -1})        (get-in @state [:units "R_H"])))
  (is (= (fjv. 2.176509252445312E-8 {"kg" 1})
         (get-in @state [:units "planckmass"])))
  (is (= (get-in @state [:units "lightspeed"])       (get-in @state [:units "c"])))
  (is (= (fjv. 341/250000 {"m" 2})       (get-in @state [:units "B10paper"])))
  (is (= (fjv. 4.67227968768 {"m" 3})    (get-in @state [:units "standard"])))
  (is (= (fjv. 884901456/244140625 {"m" 3})
         (get-in @state [:units "cord"])))
  (is (= (fjv. 0.0010571721050064 {"m" 3})
         (get-in @state [:units "chenice"])))
  (is (= (fjv. 0.042930491929835245 {"m" 1})
         (get-in @state [:units "size2ring"])))
  (is (= (fjv. 3.141592653589793 {})     (get-in @state [:units "pi"])))
  (is (= (fjv. 12.566370614359172 {})    (get-in @state [:units "sphere"])))
  (is (= (fjv. 8047590469303/42500000000000 {"m" 2 "kg" 1})
         (get-in @state [:units "lbledger"])))
  (is (= (fjv. 8047590469303/42500000000000 {"m" 2 "kg" 1})
         (get-in @state [:units "poundledgerpaper"])))
  (is (= (fjv. 45359237/9290304000 {"m" -2 "kg" 1})
         (get-in @state [:units "poundboxboard"])))
  (is (= (fjv. 13900383537887/62500000000000 {"m" 2 "kg" 1})
         (get-in @state [:units "poundbookpaper"])))
  (is (= (fjv. 1/1000 {"m" -2 "kg" 1})   (get-in @state [:units "gsm"])))
  (is (= (fjv. 531441/524288 {})         (get-in @state [:units "pythagoreancomma"])))
  (is (= (fjv. 9/8 {})                   (get-in @state [:units "majorsecond"])))
  (is (= (fjv. 0.22201759999999998 {"kg" 1 "mol" -1})
         (get-in @state [:units "radon"])))
  (is (= (fjv. 0.11232296715717348 {"m" 3})
         (get-in @state [:units "irishbarrel"])))
  (is (= (fjv. 5.1103059084E-4 {"m" 3})  (get-in @state [:units "log"])))
  )

(deftest parsed-fundamentals
  (is (get-in @state [:fundamentals "m"]))
  (is (get-in @state [:fundamentals "kg"]))
  (is (get-in @state [:fundamentals "A"]))
  (is (get-in @state [:fundamentals "dollar"]))
  )

(deftest parsed-fund-units
  (is (= "concentration_by_mass"         (get-in @state [:fundamental-units {"mol" 1 "kg" -1}])))
  (is (= "concentration_by_volume"       (get-in @state [:fundamental-units {"m" -3 "mol" 1}])))
  (is (= "area"                          (get-in @state [:fundamental-units {"m" 2}])))
  (is (= "volume"                        (get-in @state [:fundamental-units {"m" 3}])))
  (is (= "energy"                        (get-in @state [:fundamental-units {"m" 2 "kg" 1 "s" -2}])))
  (is (= "electric_charge_density"       (get-in @state [:fundamental-units {"A" 1 "s" 1 "m" -3}])))
  (is (= "moment_of_inertia")            (get-in @state [:fundamental-units {"m" 2 "kg" 1}]))
  )

(deftest parsed-prefixes
  (is (= (fjv. 1e24 {})                  (lookup-prefix "yotta")))
  (is (= (fjv. 1e24 {})                  (lookup-prefix "yottas")))
  (is (= (lookup-prefix "Y")             (lookup-prefix "yotta")))
  (is (= (fjv. 500000/499999 {})         (lookup-prefix "survey")))
  (is (= (lookup-prefix "geodetic")      (lookup-prefix "survey")))
  (is (= (fjv. 1/2 {})                   (lookup-prefix "half")))
  (is (= (fjv. 1024.0 {})                (lookup-prefix "kibi")))
  )

(deftest tok
  (is (= []                              (tokenize "")))
  (is (= [[:name "kalle"]]               (tokenize "kalle")))
  (is (= [[:comment "hello"]]            (tokenize "//hello")))
  (is (= [[:comment "hello"] [:name "world"]]
         (tokenize "//hello\nworld")))
  (is (= [[:name "yotta"] [:standalone-prefix] [:number 1e24]]
         (tokenize "yotta ::- 1ee24")))
  (is (= [[:name "y"] [:prefix] [:unit "yotta"]]
         (tokenize "y :- yotta")))
  (is (= [[:name "vel"] [:assign] [:unit "m"] [:divide] [:unit "s"]]
         (tokenize "vel := m / s")))
  (is (= [[:name "acc"] [:assign] [:unit "m"] [:divide] [:unit "s"] [:exp] [:number 2]]
         (tokenize "acc := m / s^2")))
  (is (= [[:name "length"] [:fundamental] [:unit "m"]]
         (tokenize "length  =!= m")))
  (is (= [[:name "m"] [:exp] [:number 2] [:unit-combination] [:unit "area"]]
         (tokenize "m^2 ||| area")))
  (is (= [[:name "pi"] [:assign] [:open] [:number 3.14] [:close]]
         (tokenize "pi := (3.14)")))
  (is (= [[:name "c"] [:assign] [:number 299792458] [:unit "m"] [:divide] [:unit "s"]]
         (tokenize "c :=                   299792458 m/s")))
  (is (= [[:open-bracket] [:close-bracket] [:plus] [:multiply]]
         (tokenize "[]+*")))
  (is (= [[:name "a"] [:assign] [:unit "m"] [:name "b"] [:assign] [:unit "s"]]
         (tokenize "a := m \n b := s")))
  (is (= [[:name "a"] [:assign] [:unit "b"]]
         (tokenize "a:=b")))
  (is (= [[:name "a"] [:assign] [:unit "m"] [:name "b"] [:assign] [:unit "s"]]
         (tokenize "a:=m\nb:=s")))
  )
