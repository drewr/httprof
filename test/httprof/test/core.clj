(ns httprof.test.core
  (:require [ring.adapter.jetty :as ring]
            [clojure.java.io :as io]
            [httprof.core :as http])
  (:use [clojure.test]))

(declare handler)

(defonce server (atom nil))

(defonce port
  (loop [pt 23456]
    (if-let [srv (try
                   (ring/run-jetty #'handler {:port pt :join? false})
                   (catch java.net.BindException _))]
      (do
        (reset! server srv)
        pt)
      (recur (inc pt)))))

(def url (format "http://localhost:%d" port))

(defn handler [req]
  #_(prn req)
  (condp = [(:request-method req) (:uri req)]
    [:head "/"] {:status 200}
    [:get "/"] {:status 200 :body "get"}
    [:get "/get"] {:status 200 :body "get"}
    [:get "/content-type"] {:status 200 :body (:content-type req)}
    [:get "/header"] {:status 200 :body (get-in req [:headers "x-my-header"])}
    [:post "/post"] {:status 200 :body (slurp (:body req))}
    [:get "/error"] {:status 500 :body "o noes"}
    [:get "/timeout"] (do
                        (Thread/sleep 10)
                        {:status 200 :body "timeout"})
    [:delete "/delete-with-body"] {:status 200 :body "delete-with-body"}
    [:post "/multipart"] {:status 200 :body (:body req)}))

(deftest t-alive
  (is (http/alive? url))
  (is (http/alive? url :method :get))
  (is (not (http/alive? "http://foo.bar.dom"))))

(deftest t-core
  (is (== 1 (http/realmain (io/resource "get.httprof") url 1)))
  (is (== 1 (http/realmain (io/resource "spaces.httprof") url 1)))
  (is (== 0 (http/realmain (io/resource "comments.httprof") url 1))))
