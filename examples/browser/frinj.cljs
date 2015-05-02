(ns frinj.browser.example
  (:require [frinj.core :as core]
            [frinj.ops :as ops]
            [cljs.reader :as reader]
            [goog.net.XhrIo :as xhr]
            [goog.i18n.NumberFormat]))

(enable-console-print!)

;; This operates on a page like demo.html, adding scrubbing number inputs and
;; automatically-updating outputs. To add further examples, follow these rules:
;;
;; * Elements with the `data-key` attribute are scrubbable.
;; * * These elements may also have a `data-step` attribute, indicating the scrubbing step
;;     size. This defaults to 1.
;; * * If the step size is less than 1, the output elements will be shown to the same
;;     number of decimal places as the step attribute.
;;
;; * Elements whose `for` attribute matches the `data-key` attribute of a scrubbable
;;   element will be updated in real time.
;; * * Output elements with a `data-dp` attribute will be formatted to that number of
;;     decimal places.
;; * * If the output element's parent is a `code` element, then it is assumed to be a
;;     runnable example. The output will not be formatted with thousands separators, but
;;     will still use the correct number of decimal places.
;;
;; * Elements whose `for` attributes matches the `id` attribute of a code example will be
;;   updated when the example's result changes. In other respects, these are the same as
;;   above.
;;
;; * Runnable examples are `code` elements wrapped under a common ancestor.
;; * * If the example has a sibling element with the `result` class, its output will be
;;     updated there, as well as in any output elements. If the result element also has a
;;     `static` class, it will not be updated.
;; * * If the example has a `data-next` attribute, the example with that ID will run after
;;     the example. (These can be chained.)
;; * * All examples run when their inputs change. Examples with the class `unit` are also
;;     run on page load, so the units are added if other examples need them.
;;

;; Needed because ClojureScript doesn't support eval.
(def ops-map
  {'fj ops/fj
   'fj+ ops/fj+
   'fj_ ops/fj_
   'fj* ops/fj*
   'fj** ops/fj**
   'to ops/to
   'add-unit! ops/add-unit!
   '- -
   '+ +})

;; Allow iterating over DOM elements.
(extend-type js/NodeList ISeqable (-seq [array] (array-seq array 0)))

(defn- noop [])
(defn- log [s] (println s) s)
(defn- get-data [element key] (aget element "dataset" (name key)))

(defn- format-result [number dp]
  (let [dp (or dp (if (> number 1) 0 2))
        dp-suffix (when (> dp 0) (apply str (cons "." (repeat dp "0"))))
        formatter (js/goog.i18n.NumberFormat. (str "#,##0" dp-suffix) "goog.i18n.NumberFormatSymbols_en")]
    (.format formatter number)))

;; If the text content of the element is a threaded form, ending in `str`, then only take
;; the middle part. Otherwise, run the whole thing.
;;
(defn- runnable-part [element]
  (let [sexpr (reader/read-string (.-textContent element))
        first (first sexpr)
        last (last sexpr)
        mid (-> sexpr rest butlast)]
    (if (and (= '-> first) (= 'str last)) mid sexpr)))

;; ClojureScript doesn't support eval!
;;
(defn ghetto-eval [sexpr]
  (let [[fn & args] (map #(if (list? %) (ghetto-eval %) %) sexpr)]
    (apply (or (ops-map fn) fn) args)))

;; ClojureScript doesn't support eval!!!!!
;;
(defn ghetto-thread [value fns]
  (if fns
    (let [[[fn & args] & next] fns]
      (ghetto-thread (ghetto-eval (cons fn (cons value args))) next))
    value))

(defn- run-example!
  ([element] (run-example! element false))
  ([element skip-next]
   (when-let [[data & sexprs] (runnable-part element)]
     (let [result (if (coll? data) (ghetto-thread (ghetto-eval data) sexprs)
                      (ghetto-eval (cons data sexprs)))
           id (.-id element)
           visible? (not (.contains (.-classList element) "hidden"))
           output-element (js/document.querySelector (str "[for=" id "]"))
           result-element (.querySelector (.-parentNode element) ".result:not(.static)")
           next-example-id (get-data element :next)]
       (when output-element
         (let [dp (get-data output-element :dp)]
           (set! (.-textContent output-element) (format-result (:v result) dp))))
       (when (and visible? result-element)
         (set! (.-textContent result-element) (str ";; " (.toString result))))
       (when (and next-example-id (not skip-next))
         (run-example! (js/document.querySelector (str "#" next-example-id))))))))

(defn- scrubbing-adapter [element]
  (let [key (get-data element :key)
        step-attr (or (get-data element :step) "1")
        step-dp (let [[before after] (.split step-attr ".")]
                  (if after (count after) 0))
        step (js/parseFloat step-attr)
        result-elements (js/document.querySelectorAll (str "[for=" key "]"))]
    (js-obj
     "init" noop
     "start" (fn [element]
               (js/parseFloat (.replace (.-textContent (.-node element)) "," "")))
     "end" noop
     "change" (fn [scrubbing-element value delta]
                (let [calculated-value (+ (- value delta) (* delta step))
                      bounded-value (if (< calculated-value step) step calculated-value)
                      bounded-value-str (format-result bounded-value step-dp)]
                  (set! (.-textContent (.-node scrubbing-element)) bounded-value-str)
                  (doseq [result-element result-elements]
                    (let [parent (.-parentNode result-element)]
                      (if (= "CODE" (.-nodeName parent))
                        (do
                          (set! (.-textContent result-element) (.toFixed bounded-value step-dp))
                          (run-example! parent))
                        (set! (.-textContent result-element) bounded-value-str)))))))))

(defn- add-scrubbers! []
  (doseq [param (js/document.querySelectorAll "[data-key]")]
    (js/Scrubbing. param #js{"adapter" (scrubbing-adapter param)
                             "driver" #js[js/Scrubbing.driver.Mouse js/Scrubbing.driver.Touch]})))

(defn- run-unit-examples! []
  (doseq [unit-example (js/document.querySelectorAll ".example .unit")]
    (run-example! unit-example :skip-next)))

(defn- handler [event]
  (let [body (-> event .-target .getResponseText)
        state-edn (reader/read-string (str body))]
    (core/restore-state! state-edn)
    (add-scrubbers!)
    (run-unit-examples!)))

(xhr/send "units.edn" handler "GET")
