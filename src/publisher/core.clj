(ns publisher.core
  (:require [compojure.core :as compojure]
            [compojure.route :as route]
            [ring.middleware.params :as params]
            [ring.util.response :as response]
            [hiccup.core :as hiccup]
            [clojure.string :as string])
  (:import [java.io File]
           [java.util Calendar Date]
           [java.net URLEncoder URLDecoder]))

(defn files-in-directory [directory-path]
  (reduce (fn [files file] (if (.isDirectory file)
                             (concat files (files-in-directory (.getPath file)))
                             (conj files file)))
          []
          (.listFiles (File. directory-path))))

(def directory "/Users/jukka/Downloads/kuntotiedot/68356_kiinteistojen_kuntotiedot_Vaahterakuja-Vaskivuori")

(def facebook-sdk [[:div {:id "fb-root"}]
                   [:script "(function(d, s, id) {
  var js, fjs = d.getElementsByTagName(s)[0];
  if (d.getElementById(id)) return;
  js = d.createElement(s); js.id = id;
  js.src = \"//connect.facebook.net/fi_FI/sdk.js#xfbml=1&version=v2.3\";
  fjs.parentNode.insertBefore(js, fjs);
}(document, 'script', 'facebook-jssdk'));"]])

(def like-button [:div {:class "fb-like"
                        :data-href="https://developers.facebook.com/docs/plugins/"
                        :data-layout="standard"
                        :data-action="like"
                        :data-show-faces="true"
                        :data-share="true"}])

(def disqus "<div id=\"disqus_thread\"></div>
  <script type=\"text/javascript\">
  /* * * CONFIGURATION VARIABLES * * */
  var disqus_shortname = 'kuntotiedot';
  
  /* * * DON'T EDIT BELOW THIS LINE * * */
  (function() {
  var dsq = document.createElement('script'); dsq.type = 'text/javascript'; dsq.async = true;
  dsq.src = '//' + disqus_shortname + '.disqus.com/embed.js';
  (document.getElementsByTagName('head')[0] || document.getElementsByTagName('body')[0]).appendChild(dsq);
  })();
  </script>
<noscript>Please enable JavaScript to view the <a href=\"https://disqus.com/?ref_noscript\" rel=\"nofollow\">comments powered by Disqus.</a></noscript>")

(defn page [& content]
  (hiccup/html [:html
                [:head
                 [:title "Kuntotiedot"]]
                [:body (concat facebook-sdk
                               content)]]))



(defn fix-file-name [file-name]
  (-> file-name
      (string/replace "Ñ" "ä")))

(defn file-name [file]
  (-> (.getName file)
      (fix-file-name)))

(defn date-to-map [date]
  (let [calendar (Calendar/getInstance)]
    (.setTime calendar date)
    {:year (.get calendar Calendar/YEAR)
     :month (+ 1 (.get calendar Calendar/MONTH))
     :day (.get calendar Calendar/DAY_OF_MONTH)
     :hour (.get calendar Calendar/HOUR_OF_DAY)
     :minute (.get calendar Calendar/MINUTE)
     :second (.get calendar Calendar/SECOND)}))

(defn date-string [{:keys [year month day]}]
  (str day "." month "." year))



(defn last-modified-date [file]
  (date-to-map (Date. (.lastModified file))))

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
                                                                             (fix-file-name))
                                                                         " "
                                                                         (-> (str directory "/" (URLDecoder/decode folder) "/" (URLDecoder/decode file))
                                                                             (File.)
                                                                             (last-modified-date)
                                                                             (date-string))]
                                                                        like-button
                                                                        disqus))))

(def handler (app))

