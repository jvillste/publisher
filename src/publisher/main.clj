(ns publisher.main
  (:require [ring.adapter.jetty :as jetty]
            [publisher.core :as core]
            [taoensso.timbre :as timbre]
            [taoensso.timbre.appenders.core :as appenders])
  (:gen-class))

(defn -main [& args]
  (timbre/merge-config!
   {:appenders {:spit (appenders/spit-appender {:fname "publisher.log"})}})
  
  (timbre/info "starting")
  (jetty/run-jetty core/handler {:port (read-string (first args))}))



;; development

(def server (atom nil))

(defn start []
  (when @server (.stop @server))
  (.start (Thread. (fn [] (reset! server (jetty/run-jetty core/handler {:port 3001 :join? false}))))))
