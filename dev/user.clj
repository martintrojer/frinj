(ns user
  "Tools for interactive development with the REPL. This file should
  not be included in a production build of the application."
  (:require
   [clojure.java.io :as io]
   [clojure.java.javadoc :refer (javadoc)]
   [clojure.pprint :refer (pprint print-table)]
   [clojure.reflect :refer (reflect)]
   [clojure.repl :refer (apropos dir doc find-doc pst source)]
   [clojure.set :as set]
   [clojure.string :as str]
   [clojure.test :as test]
   [clojure.tools.trace :refer (trace deftrace trace-forms trace-ns trace-vars)]
   [clojure.tools.namespace.repl :refer (refresh refresh-all)])
  (:use [frinj.core :exclude [add-unit! zero one]]
        [frinj.ops]
        [frinj.jvm]
        [frinj.feeds]))

(def system
  "A Var containing an object representing the application under
  development."
  nil)

(defn init
  "Creates and initializes the system under development in the Var
  #'system."
  []
  )

(defn start
  "Starts the system running, updates the Var #'system."
  []
  (frinj-init!)
  )

(defn stop
  "Stops the system if it is currently running, updates the Var
  #'system."
  []
  (shutdown-feeds)
  )

(defn go
  "Initializes and starts the system running."
  []
  (init)
  (start)
  :ready)

(defn reset
  "Stops the system, reloads modified source files, and restarts it."
  []
  (stop)
  (refresh :after 'user/go))

(defn run-all-my-test []
  (reset)
  (test/run-all-tests #"frinj\.*test.*"))
