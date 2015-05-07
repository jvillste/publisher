(ns publisher.core
  (:require [compojure.core :as compojure]
            [compojure.route :as route]
            [ring.middleware.params :as params]
            [ring.util.response :as response]
            [hiccup.core :as hiccup]
            [clojure.string :as string])
  (:import [java.io File]
           [java.net URLEncoder URLDecoder]))

(defn files-in-directory [directory-path]
  (reduce (fn [files file] (if (.isDirectory file)
                             (concat files (files-in-directory (.getPath file)))
                             (conj files file)))
          []
          (.listFiles (File. directory-path))))

(def directory "/Users/jukka/Downloads/kuntotiedot/68356_kiinteistojen_kuntotiedot_Vaahterakuja-Vaskivuori")

(defn page [& content]
  (hiccup/html [:html
                [:head
                 [:title "Kuntotiedot"]]
                [:body content]]))

(defn fix-file-name [file-name]
  (-> file-name
      (string/replace "Ñ" "ä")))

(defn file-name [file]
  (-> (.getName file)
      (fix-file-name)))

(defn app []
  (compojure/routes (compojure/GET "/" [] (page [:ul (for [file (.listFiles (File. directory))]
                                                       [:li [:a {:href (str "/" (URLEncoder/encode (.getName file)))}
                                                             (file-name file)]])]))
                    
                    (compojure/GET "/get/:folder/:file" [folder file] (response/file-response (str directory "/" (URLDecoder/decode folder) "/" (URLDecoder/decode file))))
                    
                    (compojure/GET "/:folder" [folder] (page [:h1 (-> (URLDecoder/decode folder)
                                                                      (fix-file-name))]
                                                             [:ul (for [file (files-in-directory (str directory "/" (URLDecoder/decode folder)))]
                                                                    [:li [:a {:href (str "/" folder "/" (URLEncoder/encode (.getName file)))}
                                                                          (file-name file)]])]))

                    (compojure/GET "/:folder/:file" [folder file] (page [:a {:href (str "/get/" folder "/" file)}
                                                                         (-> file
                                                                             (URLDecoder/decode)
                                                                             (fix-file-name))]))))

(def handler (app))

