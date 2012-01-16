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
                    #_(log/log (format "%s %s %s" (:action r)
                                       (:status (:result x))
                                       (:duration x)))
                    (swap! results conj x))
                  (.countDown latch))))
    (.await latch)
    @results))

(defn secs [millis]
  (float (/ millis 10e2)))

(defn tsplit [n threshold]
  (let [x (* n threshold)]
    [(-> (- n x) Math/floor int)
     (-> x Math/ceil int)]))

(defn -main [reqs url nconns]
  (let [nconns (BigInteger. nconns)
        pool (Executors/newFixedThreadPool nconns)
        parser (parse-fn "default")
        reqseq (->> (io/reader reqs)
                    line-seq
                    (map parser)
                    (map #(assoc % :url url)))
        res (timed (fn [] (go reqseq pool)))
        total-secs (secs (:duration res))
        nreqs (count reqseq)
        [ntop nbot] (tsplit nreqs 0.05)
        durations (->> res :result (map :duration) sort)]
    (log/log "connections:" nconns)
    (log/log "requests:" nreqs)
    (log/log "total secs:" total-secs)
    (log/log "req/secs:" (format "%.3f" (/ nreqs total-secs)))
    (log/log "min secs:" (secs (first durations)))
    (log/log "max secs:" (secs (last durations)))
    (log/log (format "max of bottom 5%% (%d): %.3f"
                     nbot
                     (secs (first (drop ntop durations)))))
    (.shutdown pool)
    (await log/logger)
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
