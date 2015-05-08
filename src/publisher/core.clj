(ns publisher.core
  (:require [compojure.core :as compojure]
            [compojure.route :as route]
            [ring.middleware.params :as params]
            [ring.middleware.file :as file]
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

(def files-directory "files")

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

(defn page [breadcrumb & content]
  (hiccup/html [:html
                [:head
                 "<meta charset=\"utf-8\">
    <meta http-equiv=\"X-UA-Compatible\" content=\"IE=edge\">
    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">"
                 
                 [:title "Vantaan kaupungin kiinteistöjen kuntotiedot"]
                 
                 "<link href=\"/bootstrap-3.3.4-dist/css/bootstrap.min.css\" rel=\"stylesheet\">

                 <!-- HTML5 shim and Respond.js for IE8 support of HTML5 elements and media queries -->
    <!-- WARNING: Respond.js doesn't work if you view the page via file:// -->
    <!--[if lt IE 9]>
      <script src=\"https://oss.maxcdn.com/html5shiv/3.7.2/html5shiv.min.js\"></script>
      <script src=\"https://oss.maxcdn.com/respond/1.4.2/respond.min.js\"></script>
    <![endif]-->"
                 [:link {:href "/publisher.css" :rel "stylesheet"}]]
                
                [:body                 
                 [:div {:class "container"}
                  [:div {:class "header"}
                   [:a {:href "/" :style "color: black"} [:h3 "Vantaan kaupungin kiinteistöjen kuntotiedot"]]
                   breadcrumb]
                  (concat facebook-sdk
                          content)]
                 

                 "<!-- jQuery (necessary for Bootstrap's JavaScript plugins) -->
    <script src=\"https://ajax.googleapis.com/ajax/libs/jquery/1.11.2/jquery.min.js\"></script>
    <!-- Include all compiled plugins (below), or include individual files as needed -->
    <script src=\"js/bootstrap.min.js\"></script>"]]))



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

(defn kiinteistö-table []
  [:table {:class "table table-striped"}
   [:thead [:tr [:th "Kiinteistö"]
            [:th "Tiedostojen määrä"]
            [:th "Uusimman tiedoston päiväys"]]]
   [:tbody (for [file (->> (.listFiles (File. files-directory))
                           (filter #(.isDirectory %)))]
             (let [files (.listFiles file)]
               [:tr [:td [:a {:href (str "/" (URLEncoder/encode (.getName file)))}
                          (file-name file)]]
                [:td (count files)]
                [:td (->> files
                          (sort-by #(.lastModified %))
                          last
                          last-modified-date
                          date-string)]]))]])


(defn file-table [folder]
  [:table {:class "table table-striped"}
   [:thead [:tr [:th "Tiedosto"]
            [:th "Viimeksi muokattu"]]]
   [:tbody (for [file (->> (files-in-directory (str files-directory "/" (URLDecoder/decode folder)))
                           (sort-by #(.lastModified %))
                           reverse)]
             [:tr [:td [:a {:href (str "/" folder "/" (URLEncoder/encode (.getName file)))}
                        (file-name file)]]
              [:td (->> file
                        last-modified-date
                        date-string)]])]])

(defn app []
  (compojure/routes (compojure/GET "/" [] (page [:div]
                                                [:div {:class "jumbotron"}
                                                 "Tässä Vantaan kaupungin valtuutetuille helmikuussa 2014 julkaisemat kiinteistöjen kuntotiedot"]
                                                (kiinteistö-table)))
                    
                    (compojure/GET "/get/:folder/:file" [folder file] (response/file-response (str files-directory "/" (URLDecoder/decode folder) "/" (URLDecoder/decode file))))
                    
                    (compojure/GET "/:folder" [folder] (let [kiinteistön-nimi (-> (URLDecoder/decode folder)
                                                                                  (fix-file-name))]
                                                         (page [:div
                                                                [:a {:href "/"} "Kaikki kiinteistöt"]
                                                                " / " kiinteistön-nimi]
                                                               
                                                               [:h1 kiinteistön-nimi]
                                                               (file-table folder)

                                                               [:h2 "Kommentit"]
                                                               disqus)))

                    (compojure/GET "/:folder/:file" [folder file] (let [kiinteistön-nimi (-> (URLDecoder/decode folder)
                                                                                             (fix-file-name))
                                                                        tiedoston-nimi (-> file
                                                                                           (URLDecoder/decode)
                                                                                           (fix-file-name))]
                                                                    (page [:div
                                                                           [:a {:href "/"} "Kaikki kiinteistöt"]
                                                                           " / " [:a {:href (str "/" folder)} kiinteistön-nimi]
                                                                           " / " tiedoston-nimi]

                                                                          
                                                                          
                                                                          [:a {:href (str "/get/" folder "/" file)}
                                                                           [:h1 {:style "text-decoration: underline"} tiedoston-nimi]]
                                                                          
                                                                          [:div {:class "pull-right"} like-button] 
                                                                          
                                                                          [:h2 "Kommentit"]
                                                                          disqus)))))

(def handler (-> (app)
                 (file/wrap-file "resources")))
