(ns publisher.core
  (:require [compojure.core :as compojure]
            [compojure.route :as route]
            [ring.middleware.params :as params]
            [ring.middleware.file :as file]
            [ring.util.response :as response]
            [hiccup.core :as hiccup]
            [clojure.string :as string]
            [publisher.classes :as classes]
            [taoensso.timbre :as timbre])
  (:import [java.io File]
           [java.util Calendar Date]
           [java.net URLEncoder URLDecoder]))

(def files-directory "files")
(def site-url "http://kuntotiedot.sirpakauppinen.fi")

  (def facebook-sdk "<div id=\"fb-root\"></div>
  <script>(function(d, s, id) {
  var js, fjs = d.getElementsByTagName(s)[0];
  if (d.getElementById(id)) return;
  js = d.createElement(s); js.id = id;
  js.src = \"//connect.facebook.net/fi_FI/sdk.js#xfbml=1&version=v2.3\";
  fjs.parentNode.insertBefore(js, fjs);
}(document, 'script', 'facebook-jssdk'));</script>")

(defn like-button [url]
  (str "<div class=\"fb-like\" data-href=\"" url "\" data-layout=\"standard\" data-action=\"like\" data-show-faces=\"true\" data-share=\"true\"></div>" ))


(defn path-to-disqus-id [path]
  (-> path
      (string/replace "," "%2C")
      URLDecoder/decode
      URLEncoder/encode))

(defn disqus [path title]
  (str "<div id=\"disqus_thread\"></div>
  <script type=\"text/javascript\">
  /* * * CONFIGURATION VARIABLES * * */
  var disqus_shortname = 'kuntotiedot';
  var disqus_identifier = '" (path-to-disqus-id path) "';
  var disqus_title = '" title "';
  var disqus_url = '" site-url "/" path "';
  /* * * DON'T EDIT BELOW THIS LINE * * */
  (function() {
  var dsq = document.createElement('script'); dsq.type = 'text/javascript'; dsq.async = true;
  dsq.src = '//' + disqus_shortname + '.disqus.com/embed.js';
  (document.getElementsByTagName('head')[0] || document.getElementsByTagName('body')[0]).appendChild(dsq);
  })();
  </script>
  <noscript>Please enable JavaScript to view the <a href=\"https://disqus.com/?ref_noscript\" rel=\"nofollow\">comments powered by Disqus.</a></noscript>"
  ))

  (def disqus-comment-count-code "<script type=\"text/javascript\">
  /* * * CONFIGURATION VARIABLES * * */
  var disqus_shortname = 'kuntotiedot';
  
  /* * * DON'T EDIT BELOW THIS LINE * * */
  (function () {
  var s = document.createElement('script'); s.async = true;
  s.type = 'text/javascript';
  s.src = '//' + disqus_shortname + '.disqus.com/count.js';
  (document.getElementsByTagName('HEAD')[0] || document.getElementsByTagName('BODY')[0]).appendChild(s);
  }());
</script>")

(def google-analythics
  "<script>
  (function(i,s,o,g,r,a,m){i['GoogleAnalyticsObject']=r;i[r]=i[r]||function(){
  (i[r].q=i[r].q||[]).push(arguments)},i[r].l=1*new Date();a=s.createElement(o),
  m=s.getElementsByTagName(o)[0];a.async=1;a.src=g;m.parentNode.insertBefore(a,m)
  })(window,document,'script','//www.google-analytics.com/analytics.js','ga');

  ga('create', 'UA-62795849-1', 'auto');
  ga('send', 'pageview');

</script>")

(defn page [title breadcrumb & content]
  (hiccup/html [:html
                [:head
                 "<meta charset=\"utf-8\">
    <meta http-equiv=\"X-UA-Compatible\" content=\"IE=edge\">
    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">"
                 
                 [:title title]
                 
                 "<link href=\"/bootstrap-3.3.4-dist/css/bootstrap.min.css\" rel=\"stylesheet\">

                 <!-- HTML5 shim and Respond.js for IE8 support of HTML5 elements and media queries -->
    <!-- WARNING: Respond.js doesn't work if you view the page via file:// -->
    <!--[if lt IE 9]>
      <script src=\"https://oss.maxcdn.com/html5shiv/3.7.2/html5shiv.min.js\"></script>
      <script src=\"https://oss.maxcdn.com/respond/1.4.2/respond.min.js\"></script>
    <![endif]-->"
                 [:link {:href "/publisher.css" :rel "stylesheet"}]]
                
                [:body
                 
                 facebook-sdk
                 
                 [:div {:class "container"}
                  [:div {:class "header"}
                   [:a {:href "/" :style "color: black"} [:h3 "Vantaan kaupungin kiinteistöjen kuntotiedot"]]
                   breadcrumb]
                  
                  content]
                 
                 google-analythics

                 "<!-- jQuery (necessary for Bootstrap's JavaScript plugins) -->
    <script src=\"https://ajax.googleapis.com/ajax/libs/jquery/1.11.3/jquery.min.js\"></script>
    <!-- Include all compiled plugins (below), or include individual files as needed -->
    <script src=\"/bootstrap-3.3.4-dist/js/bootstrap.min.js\"></script>"

                 ]]))



(defn fix-file-name [file-name]
  (-> file-name
      (string/replace "Ñ" "ä")
      (string/replace "é" "Ä")

      (string/replace "î" "ö")
      (string/replace "ô" "Ö")
      
      (string/replace "Ü" "å")
      (string/replace "è" "Å")))

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

(defn kiinteistö-table [class]
  [:table {:class "table table-striped"}
   [:thead [:tr [:th "Kiinteistö"]
            [:th "Tiedostojen määrä"]
            [:th "Uusimman tiedoston päiväys"]]]
   [:tbody (for [file (->> (.listFiles (File. files-directory))
                           (filter #(.isDirectory %))
                           (filter #(= class (get classes/classes (.getName %))))
                           (sort-by #(fix-file-name (.getName %))))]
             (let [files (->> (.listFiles file)
                              (filter #(not (.isDirectory %))))]
               
               [:tr [:td [:a {:href (str "/" (URLEncoder/encode (.getName file)))}
                          (file-name file)]]
                [:td (count files)]
                [:td (if (not (empty? files))
                       (->> files
                            (sort-by #(.lastModified %))
                            last
                            last-modified-date
                            date-string)
                       "")]]))]])


(defn file-table [folder]
  [:div [:table {:class "table table-striped"}
         [:thead [:tr [:th "Tiedosto"]
                  [:th "Viimeksi muokattu"]
                  [:th "Kommenttien määrä"]]]
         [:tbody (for [file (->> (.listFiles (File. (str files-directory "/" (URLDecoder/decode folder))))
                                 (sort-by #(.lastModified %))
                                 reverse)]
                   [:tr [:td [:a {:href (str "/" folder "/" (URLEncoder/encode (.getName file)))}
                              (file-name file)]]
                    
                    [:td (->> file
                              last-modified-date
                              date-string)]

                    [:td [:span {:class "disqus-comment-count" :data-disqus-identifier (path-to-disqus-id (str folder "/" (.getName file) ))} "-"]]])]]

   disqus-comment-count-code])



(defn show-file [folder file]
  (timbre/info (str "show file" folder "/" file))
  (let [kiinteistön-nimi (-> (URLDecoder/decode folder)
                             (fix-file-name))
        tiedoston-nimi (-> file
                           (URLDecoder/decode)
                           (fix-file-name))]

    (page (str kiinteistön-nimi " / " tiedoston-nimi)

          [:div
           [:a {:href "/"} "Kaikki kiinteistöt"]
           " / " [:a {:href (str "/" folder)} kiinteistön-nimi]
           " / " tiedoston-nimi]

          [:a {:href (str "/" folder) :style "color: black;"} [:h1 kiinteistön-nimi]] 
          
          [:a {:href (str "/get/" folder "/" file) :class "file-download-link"}
           tiedoston-nimi]

          [:div {:style "margin-left: 10px; display: inline-block"}
           "(" (-> (str "files/" folder "/" file) 
                   (URLDecoder/decode)
                   (File.)
                   last-modified-date
                   date-string)
           ")"]
          
          
          [:div {:style "margin-top: 20px"}
           (like-button (str site-url "/" folder "/" file))]
          
          [:h2 {:class "comments-header"} "Kommentit"]
          (disqus (str folder "/" file) (str kiinteistön-nimi " / " tiedoston-nimi)))))

(defn show-folder [folder]
  (timbre/info "show folder" folder)
  (let [kiinteistön-nimi (-> (URLDecoder/decode folder)
                             (fix-file-name))]
    (page (str "Kuntotiedot: " kiinteistön-nimi)

          [:div
           [:a {:href "/"} "Kaikki kiinteistöt"]
           " / " kiinteistön-nimi]
          
          [:h1 kiinteistön-nimi]

          [:div {:style "margin-top: 20px; margin-bottom: 20px"}
           (like-button (str site-url "/" folder))]
          
          (file-table folder)

          [:h2 {:class "comments-header"} "Kommentit"]
          (disqus folder kiinteistön-nimi))))


(def koulut (kiinteistö-table :koulu))
(def päiväkodit (kiinteistö-table :päiväkoti))
(def muut (kiinteistö-table nil))

(defn app []
  (compojure/routes (compojure/GET "/" [] (do (timbre/info "front page")
                                              (page "Vantaan kaupungin kiinteistöjen kuntotiedot"
                                                    ""
                                                    [:div {:class "jumbotron"}
                                                     [:p "Tällä sivustolla voit ladata ja kommentoida Vantaan kaupunginvaltuutetuille helmikuussa 2014 luovutettuja Vantaan kiinteistöjen kuntotietoja. Aineisto koostuu sekalaisista tiedostoista joiden sisältö on paikoin vaikeasti tulkittavaa. Tästä syystä olisi hyvä jos ne jotka aineistoon perehtyvät, jakaisivat tekemänsä huomion arvoiset löydöksensä kommentteina tällä sivustolla, jotta muiden olisi helpompi päästä perille kiinteistöjen tilanteesta."]
                                                     [:p "Palautetta sivuston toteutuksesta voit lähettää osoitteeseen " [:img {:src "osoite.png"}]]
                                                     [:div {:style "margin-top: 20px"}
                                                      (like-button site-url)]]
                                                    [:h2 "Koulut"]
                                                    koulut
                                                    [:h2 "Päiväkodit"]
                                                    päiväkodit
                                                    [:h2 "Muut"]
                                                    muut)))
                    
                    (compojure/GET ["/get/:folder/:file" :folder #"[^/]+" :file #"[^/]+"]  [folder file]
                                   (do (timbre/info (str "load file " folder "/" file))
                                       (response/file-response (str files-directory "/" (URLDecoder/decode folder) "/" (URLDecoder/decode file)))))
                    
                    (compojure/GET ["/:folder" :folder #"[^/]+"]  [folder]
                                   (show-folder folder))

                    (compojure/GET ["/:folder/:file" :folder #"[^/]+" :file #"[^/]+"] [folder file]
                                   (show-file folder file))))

(def handler (-> (app)
                 (file/wrap-file "resources")))
