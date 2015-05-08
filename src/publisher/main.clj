(ns publisher.main
  (:require [ring.adapter.jetty :as jetty]
            [publisher.core :as core]))

(defn -main [& args]
  (jetty/run-jetty core/handler {:port (read-string (first args))}))



;; development

(def server (atom nil))

(defn start []
  (when @server (.stop @server))
  (.start (Thread. (fn [] (reset! server (jetty/run-jetty core/handler {:port 3001 :join? false}))))))
