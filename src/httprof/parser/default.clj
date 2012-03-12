(ns httprof.parser.default
  (:require [clojure.string :as s]))

(defn parse [line]
  (if (not (.startsWith line "#"))
    (let [[tok method action body] (s/split line #"\s+" 4)]
      {:tok tok :method method :action action :body body})))

