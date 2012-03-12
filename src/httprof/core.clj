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

(defn safe-div [n d]
  (let [n (or n 0.0)
        d (or d 0.0)]
    (try (/ n d) (catch ArithmeticException e 0.0))))

(defn secs [millis]
  (float (safe-div millis 10e2)))

(defn tsplit [n threshold]
  (let [x (* n threshold)]
    [(-> (- n x) Math/floor int)
     (-> x Math/ceil int)]))

(defn alive? [url & {:as opts}]
  (try
    (http/execute (merge {:url url :method "HEAD" :action "/"
                          :conn-timeout 2000
                          :socket-timeout 2000}
                         opts))
    (catch Exception _)))

(defn run-set [reqs url nc]
  (let [nc (BigInteger. (str nc))
        pool (Executors/newFixedThreadPool nc)
        parser (parse-fn "default")
        res (timed
             (fn []
               (go (->> (io/reader reqs)
                        line-seq
                        (map parser)
                        (remove nil?)
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
             (format "rate %.3f" (safe-div nreqs total-secs))
             (format "avgrate %.3f"
                     (safe-div ntop (secs (reduce + durations))))
             (format "min %.3f" (secs (first durations)))
             (format "max %.3f" (secs (last durations)))
             (format "5%%min %.3f" (secs (first (drop ntop durations))))
             httpsumm)
    (.shutdown pool)
    (await log/logger)
    nreqs))

(defn realmain [reqs url & nconns]
  (if (alive? url)
    (reduce + 0 (for [nc nconns]
                  (run-set reqs url nc)))
    (log/log (format "%s not answering" url))))

(defn -main [reqs url & nconns]
  (apply realmain reqs url nconns)
  (shutdown-agents))
