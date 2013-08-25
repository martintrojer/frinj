;;  Copyright (c) Martin Trojer. All rights reserved.
;;  The use and distribution terms for this software are covered by the
;;  Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;;  which can be found in the file epl-v10.html at the root of this distribution.
;;  By using this software in any fashion, you are agreeing to be bound by
;;  the terms of this license.
;;  You must not remove this notice, or any other, from this software.

(ns frinj.test.samplecalc
  (:use [clojure.test]
        [frinj.ops])
  (:import frinj.core.fjv))

(defn- samplecalc-test-fixture [f]
  (frinj-init!)

  (add-unit! :burnrate
             (fj_
              (fj (- 86481 41601) :thousand :dollars)
              (fj- (fj :#2001-06-30) (fj :#2000-12-31))))

  (f))

(use-fixtures :once samplecalc-test-fixture)

(deftest samples
  (is (= (fjv. 552960/77 {})
         (fj 10 :feet 12 :feet 8 :feet :to :gallons)))

  (is (= (fjv. 2718417272832/45359237 {})
         (fj 10 :feet 12 :feet 8 :feet :water :to :pounds)))

  (is (= (fjv. 5669904625/10618817472 {})
         (->  (fj_ (fj 2 :tons)
                   (fj 10 :feet 12 :feet :water))
              (to :feet))))

  (is (= (fjv. 60224381/359040 {})
         (-> (fj_ (fj 41601 :thousand :dollars)
                  (fj :burnrate))
             (to :days))))

  (is (= "Fri Dec 14 16:41:38 GMT 2001"
         (-> (fj+ (fj :#2001-06-30)
                  (fj_ (fj 41601 :thousand :dollars)
                       (fj :burnrate)))
             to-date)))

  (is (= (fjv. 368175625/129048129 {})
         (-> (fj :half :ton) (to :barrels :water))))

  (is (= (fjv. 46037384521821/19375000000000 {})
         (-> (fj 2 :fathoms :water :gravity :barrel) (to 40 :watts :minutes))))

  (is (= (fjv. 15345794840607/11266562500000 {})
         (fj 2 :fathoms :water :gravity :barrel :to :Calories)))

  (is (= (fjv. 1163/12 {})
         (fj 2000 :Calories :per :day :to :watts)))

  (is (= (fjv. 800000000000/43161375789 {})
         (-> (fj_ (fj 1100 :W 30 :sec)
                  (fj 27 :oz 1 :calorie :per :gram :per :degC))
             (to :degF))))

  )
