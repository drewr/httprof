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

(defn timestamp []
  (let [fmt (java.text.SimpleDateFormat. "yyyy-MM-dd'T'HH:mm:ss.SSSZ")]
    (.format fmt (java.util.Date.))))

(defn hmsstamp []
  (let [fmt (java.text.SimpleDateFormat. "HH:mm:ss.SSS")]
    (.format fmt (java.util.Date.))))
