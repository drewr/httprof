(ns httprof.parser.default
  (:require [clojure.string :as s]))

(defn parse [line]
  (let [[method action body] (s/split line #"\s+" 3)]
    {:method method :action action :body body}))

