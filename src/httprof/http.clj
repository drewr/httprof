(ns httprof.http
  (:use [slingshot.slingshot])
  (:require [clj-http.client :as http]
            [httprof.log :as log]))

(defn http-fn [method]
  ;; omg why does this work from a repl but not from a shell?
  (let [m (->> method str .toLowerCase (format "http/%s"))]
    (resolve (symbol m)))

  ;; hack for now
  #'clj-http.client/post)

(defn execute [{:keys [url method action] :as opts}]
  (let [opts (merge {:throw-exceptions false} opts)
        opts (merge opts {:method (-> opts :method .toLowerCase keyword)})
        url (str url action)]
    (dissoc (http/request (merge opts {:url url})) :body)))
