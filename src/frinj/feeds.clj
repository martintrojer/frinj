;;  Copyright (c) Martin Trojer. All rights reserved.
;;  The use and distribution terms for this software are covered by the
;;  Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;;  which can be found in the file epl-v10.html at the root of this distribution.
;;  By using this software in any fashion, you are agreeing to be bound by
;;  the terms of this license.
;;  You must not remove this notice, or any other, from this software.

(ns frinj.feeds
  (:use [frinj.core]
        [frinj.calc]
        [clojure.xml :only [parse]]
        [clojure.zip :only [xml-zip children down]])
  (:import [frinj.core fjv]
           [java.util.concurrent ScheduledThreadPoolExecutor TimeUnit]))

;; =================================================================

(def feed-pool (atom (ScheduledThreadPoolExecutor. 1)))

(defn start-feed
  "Schedule a feed"
  [f period scale]
  (.scheduleAtFixedRate @feed-pool f 0 period scale))

(defn stop-feed
  [f]
  (.cancel f true))

(defn restart-feed [f period scale]
  (try
    (stop-feed f)
    (catch Exception e))
  (start-feed f period scale))

(defn shutdown-feeds
  "Shutdown all scheduled feeds"
  []
  (.shutdown @feed-pool))

;; =================================================================

(defn- fetch-url
  "Fetch data from an URL and return data as a string"
  [adr]
  (let [url (java.net.URL. adr)]
    (with-open [stream (.openStream url)]
      (let [buf (java.io.BufferedReader. (java.io.InputStreamReader. stream))]
        (apply str (line-seq buf))))))

(defn- zip-str [s]
  (xml-zip (parse (java.io.ByteArrayInputStream. (.getBytes s)))))

(defn update-units!
  "Updates the frinj state with provided units. Unit-map in the form {target-unit [rate source-unit]}"
  [unit-map]
  (loop [[[cur [rate src]] & rst] (vec unit-map)]
    (when @*debug* (println "update-units!" cur rate src))
    (when cur
      (add-unit! cur (fj* rate src))
      (recur rst))))

;; =================================================================

(defn- get-themoneyconverter-com-rates []
  (let [get-target (fn [r] (-> (filter #(= (:tag %) :title) r)
                              first
                              :content
                              first
                              (.split "/")
                              first))
        get-rate (fn [r] (->> (filter #(= (:tag %) :description) r)
                             first
                             :content
                             first
                             (re-seq #"[0-9\.]+")
                             second
                             read-string))
        rates (->> "http://themoneyconverter.com/rss-feed/USD/rss.xml"
                   fetch-url
                   zip-str
                   down
                   children
                   (filter #(= (:tag %) :item))
                   (map :content))]
    (reduce (fn [acc r] (assoc acc (get-target r) [(/ 1 (get-rate r)) (fj :USD)]))
            {} rates)))

(comment
  [{:tag :title, :attrs nil, :content ["ARS/USD"]}
   {:tag :link, :attrs nil, :content ["http://themoneyconverter.com/USD/ARS.aspx"]}
   {:tag :guid, :attrs nil, :content ["aa3d0bcc-8e6b-4b4a-9a83-256319785fbb"]}
   {:tag :pubDate, :attrs nil, :content ["Wed, 14 Mar 2012 07:08:20 GMT"]}
   {:tag :description, :attrs nil, :content ["1 US Dollar = 4.34709 Argentine Peso"]}
   {:tag :category, :attrs nil, :content ["South America"]}])

(defn- update-themoneyconverter-com-rates []
  (add-unit! "USD" (fj :dollar))
  (update-units! (get-themoneyconverter-com-rates)))

(defn restart-exchange-feed! []
  (restart-feed update-themoneyconverter-com-rates 15 TimeUnit/MINUTES))

;; =================================================================

(defn- capitalize [s]
  (str (.toUpperCase (.substring s 0 1)) (.substring s 1)))

(defn- get-xmlcharts-com-rates [url]
  (let [get-target (fn [r] (capitalize (:commodity (:attrs r))))
        get-rate (fn [r] (-> r
                            :content
                            first
                            read-string))
        get-weight (fn [r] (->
                           (:per (:attrs r))
                           (.replace " " "-")))
        rates (->> url
                   fetch-url
                   zip-str
                   children
                   first            ;; TODO; not good, filter for usd here
                   :content)]

    (reduce (fn [acc r] (assoc acc (get-target r) [(get-rate r) (fj :dollar :per (get-weight r))]))
            {} rates)))

(comment
  [{:tag :price, :attrs {:timestamp "1331732940", :per "ozt", :commodity "gold"}, :content ["1647.80"]}
   {:tag :price, :attrs {:timestamp "1331732760", :per "ozt", :commodity "palladium"}, :content ["699.00"]}
   {:tag :price, :attrs {:timestamp "1331732880", :per "ozt", :commodity "platinum"}, :content ["1679.50"]}
   {:tag :price, :attrs {:timestamp "1331732940", :per "ozt", :commodity "silver"}, :content ["32.88"]}])

(defn- update-precious-metal-rates []
  (add-unit! "ozt" (fj :oz))
  (update-units! (get-xmlcharts-com-rates "http://www.xmlcharts.com/cache/precious-metals.xml")))

(defn- update-industrial-metal-rates []
  (update-units! (get-xmlcharts-com-rates "http://www.xmlcharts.com/cache/industrial-metals.xml")))

(defn- update-agrarian-rates []
  (add-unit! "short-hundredweight" (fj 100 :lb))
  (update-units! (get-xmlcharts-com-rates "http://www.xmlcharts.com/cache/agrarian.xml")))

(defn restart-precious-metal-feed! []
  (restart-feed update-precious-metal-rates 1 TimeUnit/HOURS))

(defn restart-industrial-metal-feed! []
  (restart-feed update-industrial-metal-rates 1 TimeUnit/HOURS))

(defn restart-agrarian-feed! []
  (restart-feed update-agrarian-rates 1 TimeUnit/HOURS))

;; =================================================================

;; historical
;; http://www.measuringworth.com/ppoweruk/result.php?year_result=2005&amount=1&use%5B%5D=CPI&year_source=
