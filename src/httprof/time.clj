(ns httprof.time)

(defn now []
  (System/currentTimeMillis))

(defn timed [f]
  (let [start (now)
        result (f)
        end (now)]
    {:result result
     :start start
     :end end
     :duration (- end start)}))

