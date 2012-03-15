;;  Copyright (c) Martin Trojer. All rights reserved.
;;  The use and distribution terms for this software are covered by the
;;  Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;;  which can be found in the file epl-v10.html at the root of this distribution.
;;  By using this software in any fashion, you are agreeing to be bound by
;;  the terms of this license.
;;  You must not remove this notice, or any other, from this software.

(ns frinj.test.core
  (:use [clojure.test])
  (:use [frinj.core])  
  (:import [frinj.core fjv]))

(defn- core-test-fixture [f]
  (reset-states!)
  (f))

(use-fixtures :each core-test-fixture)

(deftest add-u
  (do
    (add-unit! "one" one)    
    (is (= @units            {"one" one "ones" one})))
  )

(deftest add-u2
  (do
    (add-unit! "his" one)
    (is (= @units            {"his" one}))))
  

(deftest add-u3
  (do
    (add-unit! "k" one)    
    (is (= @units            {"k" one})))
  )

(deftest add-us
  (is (= {}                  (add-units)))
  (is (= {}                  (add-units {})))
  (is (= {:m 1}              (add-units {:m 1})))
  (is (= {:m 1 :s 1}         (add-units {:m 1 :s 1})))
  (is (= {:m 3 :b 0}         (add-units {:m 1} {:m 2 :b 0})))
  (is (= {:m 6 :watts 23}    (add-units {:m 1} {:m 2} {:m 3 :watts 23})))
  (is (= {:m 0}              (add-units {:m 1} {:m -1})))
  (is (= {:m 1}              (add-units {:m -1} {:m 2})))
  )

(deftest clean
  (is (= one                 (clean-units one)))
  (is (= (fjv. 1 {:m 1})     (clean-units (fjv. 1 {:m 1}))))
  (is (= (fjv. 1 {:m 1})     (clean-units (fjv. 1 {:m 1 :s 0}))))
  (is (= one                 (clean-units (fjv. 1 {:m 0 :s 0}))))
  (is (= (fjv. 1 {:m -1})    (clean-units (fjv. 1 {:m -1, :s 0, :k 0}))))
  )

(deftest add
  (is (nil?                  (fj-add)))
  (is (= one                 (fj-add one)))
  (is (= (fjv. 2 {})         (fj-add one 1)))
  (is (= (fjv. 1 {:m 1})     (fj-add (fjv. 1 {:m 1}))))
  (is (= (fjv. 2 {:m 1})     (fj-add (fjv. 1 {:m 1}) (fjv. 1 {:m 1}))))
  (is (= (fjv. 0 {:m 1})     (fj-add (fjv. 1 {:m 1}) (fjv. -1 {:m 1}))))
  (is (= (fjv. 2 {:m 1 :s -1})
         (fj-add (fjv. 1 {:m 1 :s -1}) (fjv. 1 {:m 1 :s -1}))))
  (is (= (fjv. 3 {})         (fj-add one one one)))
  (is (thrown? Exception     (fj-add (fjv. 1 {:m 1}) one)))
  (is (thrown? Exception     (fj-add (fjv. 1 {:m 1}) (fjv. 1 {:s 1}))))

  (is (= (fjv. 2 {})         (fj-add 1 1)))
  (is (= (fjv. 2 {})         (fj-add 1 one)))
  (is (thrown? Exception     (fj-add 1 (fjv. 1 {"m" 1}))))
  )

(deftest sub
  (is (nil?                  (fj-sub)))
  (is (= one                 (fj-sub one)))
  (is (= zero                (fj-sub one 1)))
  (is (= (fjv. 1 {:m 1})     (fj-sub (fjv. 1 {:m 1}))))
  (is (= (fjv. 0 {:m 1})     (fj-sub (fjv. 1 {:m 1}) (fjv. 1 {:m 1}))))
  (is (= (fjv. 2 {:m 1})     (fj-sub (fjv. 1 {:m 1}) (fjv. -1 {:m 1}))))
  (is (= (fjv. 0 {:m 1 :s -1})
         (fj-sub (fjv. 1 {:m 1 :s -1}) (fjv. 1 {:m 1 :s -1}))))
  (is (= (fjv. -1 {})        (fj-sub one one one)))
  (is (thrown? Exception     (fj-sub (fjv. 1 {:m 1}) one)))
  (is (thrown? Exception     (fj-sub (fjv. 1 {:m 1}) (fjv. 1 {:s 1}))))
  )

(deftest mul
  (is (nil?                  (fj-mul)))
  (is (= one                 (fj-mul one)))
  (is (= one                 (fj-mul one one)))
  (is (= one                 (fj-mul one 1)))
  (is (= (fjv. 1 {:m 2})     (fj-mul (fjv. 1 {:m 1}) (fjv. 1 {:m 1}))))
  (is (= (fjv. 1 {:m 0})     (fj-mul (fjv. 1 {:m 1}) (fjv. 1 {:m -1}))))
  (is (= (fjv. 9 {:m 1 :s -1})
         (fj-mul (fjv. 3 {:m 1}) (fjv. 3 {:s -1}))))
  (is (= (fjv. 3.14 {:m 1})
         (fj-mul one (fjv. 3.14 {:m 1}))))
  (is (= (fjv. 0 {:m 1})     (fj-mul (fjv. 3 {:m 1}) zero)))

  (is (= (fjv. 2 {})         (fj-mul 1 2)))
  (is (= (fjv. 2 {})         (fj-mul 1 (fjv. 2 {}))))
  (is (= (fjv. 2 {"m" 1})    (fj-mul 2 (fjv. 1 {"m" 1}))))
  )

(deftest div
  (is (nil?                  (fj-div)))
  (is (= one                 (fj-div one)))
  (is (= one                 (fj-div one one)))
  (is (= one                 (fj-div one 1)))
  (is (= (fjv. 1 {:m 0})     (fj-div (fjv. 1 {:m 1}) (fjv. 1 {:m 1}))))
  (is (= (fjv. 1 {:m -2})    (fj-div one (fjv. 1 {:m 2}))))
  (is (= (fjv. 1/2 {:m 1})   (fj-div one (fjv. 2 {:m -1}))))
  (is (= (fjv. 3 {:m 1 :s -1})
         (fj-div (fjv. 1 {:m 1}) (fjv. 1/3 {:s 1}))))
  (is (= (fjv. 9 {:m 1})     (fj-div one (fjv. 1/9 {:m -1}))))
  (is (thrown? Exception     (fj-div one zero)))
  (is (= (fjv. 0 {:m -1})    (fj-div zero (fjv. 1 {:m 1}))))
  )

(deftest power
  (is (= one                 (fj-int-pow one 1)))
  (is (= one                 (fj-int-pow one 0)))
  (is (= (fjv. 1 {:m 2})     (fj-int-pow (fjv. 1 {:m 1}) 2)))
  (is (= (fjv. 8 {:m -3})    (fj-int-pow (fjv. 2 {:m -1}) 3)))
  (is (= (fjv. 4 {:m 2 :s -2})
         (fj-int-pow (fjv. 2 {:m 1 :s -1}) 2)))
  (is (thrown? Exception     (fj-int-pow one 0.5)))
  (is (= one                 (fj-int-pow one -1)))
  (is (= (fjv. 4 {})         (fj-int-pow (fjv. 1/2 {}) -2)))
  (is (= (fjv. 1/4 {:m -2})
         (fj-int-pow (fjv. 2 {:m 1}) -2)))
  (is (= (fjv. 1 {:m 3})     (fj-int-pow (fjv. 1 {:m -1}) -3)))
  (is (thrown? Exception     (fj-int-pow zero -1)))
  )

(deftest inverse
  (is (= one                 (fj-inverse one)))
  (is (= (fjv. 1/2 {"m" -1 "s" 1})
         (fj-inverse (fjv. 2 {"m" 1 "s" -1}))))
  )

(deftest equal
  (is (fj-equal? one))
  (is (fj-equal? one one))
  (is (fj-equal? one one one))
  (is (not (fj-equal? one zero)))
  (is (not (fj-equal? one one zero)))
  (is (fj-equal? (fjv. 1 {:m 1}) (fjv. 1 {:m 1})))
  (is (fj-equal? (fjv. 1.1 {:m 1}) (fjv. 1.1 {:m 1})))
  (is (fj-equal? (fjv. 1/2 {:s -1 :m 1}) (fjv. 1/2 {:m 1 :s -1})))
  (is (fj-equal? (fjv. 1 {:m 1}) (fjv. 1 {:m 1 :s 0})))
  )

(deftest not-equal
  (is (not (fj-not-equal? one)))
  (is (not (fj-not-equal? one one)))
  (is (not (fj-not-equal? one one one)))
  (is (fj-not-equal? one zero))
  (is (fj-not-equal? one one zero))
  )

(deftest less
  (is (fj-less? one))
  (is (fj-less? zero one))
  (is (fj-less? zero one (fjv. 2 {})))  
  (is (not (fj-less? one one)))
  (is (not (fj-less? one zero)))
  )

(deftest greater
  (is (fj-greater? one))
  (is (fj-greater? one zero))
  (is (fj-greater? (fjv. 2 {}) one zero))  
  (is (not (fj-greater? one one)))
  (is (not (fj-greater? zero one)))
  )

(deftest less-or-equal
  (is (fj-less-or-equal? one))
  (is (fj-less-or-equal? one one))
  (is (fj-less-or-equal? zero one))
  (is (fj-less-or-equal? zero one (fjv. 2 {})))  
  (is (not (fj-less-or-equal? one zero)))
  (is (fj-less-or-equal? (fjv. 1.1 {:m 1}) (fjv. 1.1 {:m 1})))
  )

(deftest greater-or-equal
  (is (fj-greater-or-equal? one))
  (is (fj-greater-or-equal? one zero))
  (is (fj-greater-or-equal? (fjv. 2 {}) one zero))  
  (is (fj-greater-or-equal? one one))
  (is (not (fj-greater-or-equal? zero one)))
  (is (fj-greater-or-equal? (fjv. 1.1 {:m 1}) (fjv. 1.1 {:m 1})))
  )
