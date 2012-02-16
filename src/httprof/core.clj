(ns httprof.core
  (:gen-class)
  (:use [httprof.time :only [hmsstamp timed]])
  (:require [clojure.java.io :as io]
            [httprof.http :as http]
            [httprof.log :as log]
            [httprof.parser.default])
  (:import (java.util.concurrent Executors)))

(defn parse-fn [p]
  (->> p (format "httprof.parser.%s/parse") symbol resolve))

(defn go [reqseq pool]
  (let [results (atom [])
        c (atom 0)]
    (doseq [r reqseq]
      (.execute pool
                (fn []
                  (let [x (timed #(http/execute r))]
                    #_(log/log (format "%s %s %s" (:action r)
                                       (:status (:result x))
                                       (:duration x)))
                    (swap! results conj x))
                  (swap! c inc))))
    (.shutdown pool)
    (while (not (.isTerminated pool)) (Thread/sleep 100))
    @results))

(defn secs [millis]
  (float (/ millis 10e2)))

(defn tsplit [n threshold]
  (let [x (* n threshold)]
    [(-> (- n x) Math/floor int)
     (-> x Math/ceil int)]))

(defn alive? [url]
  (try
    (http/execute {:url url :method "HEAD" :action "/"
                   :conn-timeout 2000
                   :socket-timeout 2000})
    (catch Exception _)))

(defn -main [reqs url & nconns]
  (if (alive? url)
    (doseq [nc nconns]
      (let [nc (BigInteger. nc)
            pool (Executors/newFixedThreadPool nc)
            parser (parse-fn "default")
            res (timed
                 (fn []
                   (go (->> (io/reader reqs)
                            line-seq
                            (map parser)
                            (map #(assoc % :url url)))
                       pool)))
            total-secs (secs (:duration res))
            httpsumm (http/summary-string (->> res :result (map :result)))
            nreqs (count (:result res))
            [ntop nbot] (tsplit nreqs 0.05)
            durations (->> res :result (map :duration) sort)]
        (log/log (hmsstamp)
                 "conns" nc
                 "reqs" nreqs
                 (format "secs %.3f" total-secs)
                 (format "rate %.3f" (/ nreqs total-secs))
                 (format "avgrate %.3f" (/ ntop (secs (reduce + durations))))
                 (format "min %.3f" (secs (first durations)))
                 (format "max %.3f" (secs (last durations)))
                 (format "5%%min %.3f" (secs (first (drop ntop durations))))
                 httpsumm)
        (.shutdown pool)
        (await log/logger)))
    (log/log (format "%s not answering" url)))
  (shutdown-agents))
