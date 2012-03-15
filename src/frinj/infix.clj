;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; File     : infix.clj
;; Function : Infix Math library
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Copyright (c) 2008, J. Bester
;; All rights reserved.
;;
;; Redistribution and use in source and binary forms, with or without
;; modification, are permitted provided that the following conditions are met:
;;     * Redistributions of source code must retain the above copyright
;;       notice, this list of conditions and the following disclaimer.
;;     * Redistributions in binary form must reproduce the above copyright
;;       notice, this list of conditions and the following disclaimer in the
;;       documentation and/or other materials provided with the distribution.
;;
;; THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDER ``AS IS'' AND ANY
;; EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
;; WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
;; DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER BE LIABLE FOR ANY
;; DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
;; (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
;; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
;; ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
;; (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
;; SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(ns ^{:doc "Library for converting infix mathematical formula to prefix expressions"
       :author "J. Bester"}
  frinj.infix
  (:use [frinj.core]))

;; operator precedence for formula macro
(def +precedence-table+ (ref {}))

;; symbol translation for symbols in formula 
;; (only supports binary operators)
(def +translation-table+ (ref {}))

(def +highest-precedence+ (ref 0))

(defn defop
  "Define operators for formula macro"
  ([op prec & [trans]]
     (dosync (ref-set +precedence-table+ (assoc @+precedence-table+ op prec)))
     (when-not (nil? trans)
       (dosync (ref-set +translation-table+ (assoc @+translation-table+ op trans))))
     (dosync (ref-set +highest-precedence+ (apply max (map val @+precedence-table+))))))


;; == operators ==
(defop '== 30 fj-equal?)
(defop '!= 30 fj-not-equal?)
(defop '< 40 fj-less?)
(defop '> 40 fj-greater?)
(defop '<= 40 fj-less-or-equal?)
(defop '>= 40 fj-greater-or-equal?)

(defop '- 60 fj-sub)
(defop '+ 60 fj-add)
(defop '/ 80 fj-div)
(defop '* 80 fj-mul)
(defop '** 100 fj-int-pow)

(defn- operator?
  "Check if is valid operator"
  ([sym]
     (not (nil? (get @+precedence-table+ sym)))))

(defn- find-lowest-precedence
  "find the operator with lowest precedence; search from left to right"
  ([col]
     ;; loop through terms in the coluence
     (loop [idx 0
	    col col
	    lowest-idx nil
	    lowest-prec @+highest-precedence+]
       ;; nothing left to process
       (if (empty? col)
	 ;; return lowest found
	 lowest-idx
	 ;; otherwise check if current term is lower
	 (let [prec (get @+precedence-table+ (first col))]
	   ;; is of lower or equal precedence
	   (if (and prec (<= prec lowest-prec))
	     (recur (inc idx) (rest col)
		    idx prec)
	     ;; is of high precedence therefore skip for now
	     (recur (inc idx) (rest col)
		    lowest-idx lowest-prec)))))))

(defn- translate-op
  "Translation of symbol => symbol for binary op allows for
user defined operators"
  ([op] 
     (if (contains? @+translation-table+ op)
       (get @+translation-table+ op)
       op)))

(defn infix-to-prefix
  "Convert from infix notation to prefix notation"
  ([col]
     (cond 
      ;; handle term only
      (not (seq? col)) col
      ;; handle sequence containing one term (i.e. handle parens)
      (= (count col) 1) (infix-to-prefix (first col))
      ;; handle all other cases
      true (let [lowest (find-lowest-precedence col)]
	     (if (nil? lowest) ;; nothing to split
	       col
	       ;; (a b c) bind a to hd, c to tl, and b to op
	       (let [[hd [op & tl]] (split-at lowest col)]
		 ;; recurse
		 (list (translate-op op)
		       (infix-to-prefix hd) 
		       (infix-to-prefix tl))))))))

(defmacro $=
  "Convert from infix notation to prefix notation"
  ([& equation]
     (infix-to-prefix equation)))

