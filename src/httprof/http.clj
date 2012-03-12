(ns httprof.http
  (:use [slingshot.slingshot])
  (:require [clj-http.client :as http]
            [httprof.log :as log]))

(def STATPREFIXES {"1xx" 0
                   "2xx" 0
                   "3xx" 0
                   "4xx" 0
                   "5xx" 0})

(defn http-fn [method]
  ;; omg why does this work from a repl but not from a shell?
  (let [m (->> method str .toLowerCase (format "http/%s"))]
    (resolve (symbol m)))

  ;; hack for now
  #'clj-http.client/post)

(defn execute [{:keys [url method action] :as opts}]
  (let [opts (merge {:throw-exceptions false} opts)
        opts (update-in opts [:method] #(-> % name .toLowerCase keyword))
        url (str url action)
        opts (merge opts {:url url})]
    (dissoc (http/request opts) :body)))

(defn norm-status [status]
  (let [n (-> status (/ 100) Math/floor (* 100) int str)]
    (str (first (take 1 n)) "xx")))

(defn summary [responses]
  (let [f (fn [a x]
            (update-in a [(norm-status (:status x))] (fnil inc 0)))]
    (->> (reduce f STATPREFIXES responses) seq flatten (apply sorted-map))))

(defn summary-string [responses]
  (let [summ (summary responses)]
    (apply str (interpose " " (for [[k n] summ] (format "%s %d" k n))))))
