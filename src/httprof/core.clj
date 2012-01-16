(ns httprof.core
  (:gen-class)
  (:use [httprof.time :only [timed]])
  (:require [clojure.java.io :as io]
            [httprof.http :as http]
            [httprof.log :as log]
            [httprof.parser.default])
  (:import (java.util.concurrent Executors)))

(defn parse-fn [p]
  (->> p (format "httprof.parser.%s/parse") symbol resolve))

(defn go [reqseq pool]
  (let [results (atom [])
        latch (java.util.concurrent.CountDownLatch. (count reqseq))]
    (doseq [r reqseq]
      (.execute pool
                (fn []
                  (let [x (timed #(http/execute r))]
                    (log/log (format "%s %s %s" (:action r)
                                     (:status (:result x))
                                     (:duration x)))
                    (swap! results conj x))
                  (.countDown latch))))
    (.await latch)
    @results))

(defn -main [reqs url nconns & {:as opts}]
  (let [nconns (BigInteger. nconns)
        pool (Executors/newFixedThreadPool nconns)
        parser (parse-fn (:parser opts "default"))
        reqseq (->> (io/reader reqs)
                    line-seq
                    (map parser)
                    (map #(assoc % :url url)))
        res (timed (fn [] (go reqseq pool)))]
    (log/log "** total:" (:duration res))
    (.shutdown pool)
    (shutdown-agents)))


(comment
  (def urls ["http://www.google.com"
             "http://www.cnn.com"
             "http://www.yahoo.com"
             "http://www.nytimes.com"])

  (map (comp :duration deref)
       (for [u urls]
         (future (timed #(http/get u)))))

  )
