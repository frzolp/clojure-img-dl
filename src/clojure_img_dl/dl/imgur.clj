; clojure-img-dl - an Imgur album downloader written in Clojure
; Copyright (C) 2016 Francis Zolp
;
; This program is free software: you can redistribute it and/or modify it under
; the terms of the GNU General Public License as published by the Free Software
; Foundation, either version 3 of the License, or (at your option) any later
; version.
;
; This program is distributed in the hope that it will be useful, but WITHOUT
; ANY WARRANTY; without even the implied warranty of  MERCHANTABILITY or FITNESS
; FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
;
; You should have received a copy of the GNU General Public License along with
; this program.  If not, see <http://www.gnu.org/licenses/>.
(ns clojure-img-dl.dl.imgur
  (:require [clojure.data.json :as json]
            [clj-http.client :as client]
            [clojure.java.io :as io]
            [clojure.string :as string]
            [clojurewerkz.propertied.properties :as p]
            [clojure-img-dl.util.download :as dl])
  (:gen-class))

; Client ID needed to interact with imgur's API
(def ^{:private true} auth-key (str "Client-ID " (-> (io/resource "apikey.properties")
                                    (p/load-from)
                                    (p/properties->map true)
                                    (:apikey))))

; Base URL for API calls
(def ^{:private true} api-base "https://api.imgur.com/3/")

(defn- get-album
  "Request an album's information from imgur and convert the JSON to a map"
  [album-id]
  (-> (str api-base "/album/" album-id) ; Assemble the API version of the URL
      (client/get {:headers {:authorization auth-key}}) ; Call the API endpoint with authorization key
      :body ; Get the body of the response
      (json/read-str :key-fn keyword) ; Parse the body's JSON into a map
      :data)) ; Return the data field

(defn- get-album-title
  "Gets a title for the album, using the title field if available, falls back to album ID"
  [album]
  (if (not (nil? (:title album))) ; Does the album have a title?
    (string/trim (string/replace (if (> (count (:title album)) 30) ; If the title is over 30 chars, trim it
                                   (subs (:title album) 0 30)
                                   (:title album))
                                 #"[<>:\"/\\\|\?\*\+\-\^\$\(\)\.,;\%\r\n]" "")) ; Strip out invalid character and return
    (:id album))) ; Otherwise, use the raw imgur album ID

(defn- get-file-name
  "Creates a file name for the image, as a three digit index,
  then the title, description, or image ID.
  (ex. \"001_test image.jpg\", \"002_abCDeFG.png\")"
  [image index fmt]
  (str (format fmt index) "_"
       (if (not (nil? (:title image))) ; Use the image title field if available
         (string/trim (string/replace (if (> (count (:title image)) 30) ; If the title is over 30 chars, trim it
                                        (subs (:title image) 0 30)
                                        (:title image))
                                      #"[<>:\"/\\\|\?\*\+\-\^\$\(\)\.,;\%\r\n]" "")) ; Strip out invalid characters
         (if (not (nil? (:description image))) ; If the title is not available, use the description
           (string/trim (string/replace (if (> (count (:description image)) 30) ; If the desc is over 30 chars, trim it
                                          (subs (:description image) 0 30)
                                          (:description image))
                                        #"[<>:\"/\\\|\?\*\+\-\^\$\(\)\.,;\%\r\n]" "")) ; Strip out invalid characters
           (-> (:link image) ; Without a title or description, use imgur's file name
               (string/split #"/") ; Split the field by forward slash
               last ; The last item will be the file name + extension
               (string/split #"\.") ; Split the string by period
               first))) ; The first item will be file name
       "." (-> (:link image) ; Get the file extension from the link
               (string/split #"/")
               last
               (string/split #"\.")
               last))) ; The last item will be the file extension

(defn- fix-album-id
  "Converts imgur album URLs to album IDs.
  \"https://imgur.com/a/abcDEF\" returns \"abcDEF\""
  [album-id]
  (let [split-id (string/split album-id #"/")]
    (get split-id (+ (.indexOf split-id "a") 1)))) ; The album ID follows the "a" item

(defn- get-urls-paths
  [album-id]
  (let [album (get-album (fix-album-id album-id))]
    (loop [images (:images album)
           paths '[]  ; File paths will be appended on each iteration
           links '[]] ; File URLs will be appended on each iteration
      (if (not (empty? images))
        (recur (rest images) ; Restart the loop with the remaining images and add path/link to vectors
               (conj paths (dl/create-save-path "imgur" (get-album-title album) ; Add current image destination path
                                                (get-file-name (first images)
                                                               (+ 1 (- (count (:images album))
                                                                       (count images)))
                                                               (str "%0" (count (str
                                                                                  (count (:images album))))
                                                                    "d"))))
               (conj links (:link (first images)))) ; Add current image source URL
        {:urls links :paths paths}))))

(defn save-album
  "Creates lists of image sources and destinations then executes a parallel download."
  [album-id]
  (let [dl-info (get-urls-paths album-id)]
    (time (dl/parallel-save-image (:paths dl-info) (:urls dl-info))))) ; execute the parallel save
