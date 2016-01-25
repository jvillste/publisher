(ns publisher.date-copy
  (:require [publisher.core :as core]
            [clojure.string :as string])
  (:import [java.io File]
           [java.util Date]
           [java.util Calendar Date]
           [java.net URLEncoder URLDecoder]))

(def files-directory "files")

(defn files-in-directory [directory-path]
  (reduce (fn [files file] (if (.isDirectory file)
                             (concat files (files-in-directory (.getPath file)))
                             (conj files file)))
          []
          (.listFiles (File. directory-path))))

(defn relative-path [file]
  (let [[directory file-name] (->> (string/split (.getPath file) #"/")
                                   (take-last 2))]
    (str directory "/" file-name)))

(defn save-dates []
  (let [dates (reduce (fn [dates file]
                        (assoc dates (relative-path file)  (core/last-modified-date file)))
                      {}
                      (files-in-directory files-directory))]
    (spit "dates.clj" dates)))

(defn fix-directory-names []
  (doseq [file (.listFiles (File. files-directory))]
    (let [new-file (File. (core/fix-file-name (.getPath file)))]
      (.renameTo file new-file))))

(defn fix-file-names []
  (fix-directory-names)
  (doseq [file (files-in-directory files-directory)]
    (let [new-name (core/fix-file-name (.getPath file))
          {:keys [year month day]} (core/last-modified-date file)]
      (.renameTo file (File. new-name))
      (.setLastModified (File. new-name) (.getTime (Date. (- year 1900) (- month 1) day)))
      #_(println "fixed"  new-name))))

(defn set-dates []
  (let [dates (read-string (slurp "dates.clj"))]
    (doseq [path (keys dates)]
      (let [{:keys [year month day]} (get dates path)]
        (.setLastModified (File. (str files-directory "/" path)) (.getTime (Date. (- year 1900) (- month 1) day)))))))


