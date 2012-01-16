(ns httprof.log)

(def logger (agent nil))

(defn log [& s]
  (let [s (apply str (interpose " " s))]
    (send-off logger #(do % (println s) (flush)))))

