(defproject publisher "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [ring/ring-core "1.3.2"]
                 [ring/ring-jetty-adapter "1.3.2"]
                 [compojure "1.3.3"]
                 [hiccup "1.0.5"]
                 [com.taoensso/timbre "4.0.2"]]
  :aot [publisher.main]
  :plugins [[lein-ring "0.8.11"]]
  :ring {:handler publisher.core/handler}
  :main publisher.main)
