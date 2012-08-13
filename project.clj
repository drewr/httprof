(defproject httprof "2.4"
  :description "FIXME: write description"
  :dependencies [[org.clojure/clojure "1.4.0"]
                 [clj-http "0.5.2"]]
  :profiles {:dev {:dependencies [[ring/ring-jetty-adapter "1.1.2"]
                                  [ring/ring-devel "1.1.2"]]}
             :1.3 {:dependencies [[org.clojure/clojure "1.3.0"]]}
             :1.5 {:dependencies [[org.clojure/clojure "1.5.0-alpha3"]]}}
  :aliases {"all" ["with-profile" "dev:1.3,dev:1.5,dev"]}
  :main httprof.core)
