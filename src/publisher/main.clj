(ns publisher.main
  (:require [ring.adapter.jetty :as jetty]
            [publisher.core :as core]))

(defn -main [& args]
  (jetty/run-jetty core/handler {:port (read-string (first args))}))


(comment
  (.start (Thread. (fn [] (-main "3000")))))
