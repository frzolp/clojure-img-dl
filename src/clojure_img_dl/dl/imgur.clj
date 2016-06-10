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
            [clojure-img-dl.util.download :as dl]
            [clojure.string :as str])
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
  (-> (str api-base "/album/" album-id)                     ; Assemble the API version of the URL
      (client/get {:headers {:authorization auth-key}})     ; Call the API endpoint with authorization key
      :body                                                 ; Get the body of the response
      (json/read-str :key-fn keyword)                       ; Parse the body's JSON into a map
      :data))                                               ; Return the data field

(defn- shorten-and-clean
  "Trims down long album/image names and removes any invalid characters"
  [input-name]
  (string/trim (string/replace (if (> (count input-name) 30)
                                 (subs input-name 0 30)
                                 input-name)
                               #"[<>:\"/\\\|\?\*\+\-\^\$\(\)\.,;\%\r\n]" "")))

(defn- get-album-title
  "Gets a title for the album, using the title field if available, falls back to album ID"
  [album]
  (let [album-name (if (not (nil? (:title album)))          ; Is there a title?
                     (:title album)                         ; Use the title
                     (:id album))]                          ; No title, use the imgur id
    (shorten-and-clean album-name)))



(defn- link-or-mp4
  "Returns an animated image's MP4 URL if requested,
  otherwise returns the image's default link"
  [image use-mp4]
  (if (and (true? use-mp4) (true? (:animated image)))       ; If we want MP4 and image is animated
    (:mp4 image)                                            ; Use the MP4 link
    (:link image)))                                         ; Otherwise, use the default link

(defn- get-file-name
  "Creates a file name for the image, as a three digit index,
  then the title, description, or image ID.
  (ex. \"001_test image.jpg\", \"002_abCDeFG.png\")"
  [image index fmt use-mp4]
  (let [file-suffix (-> (link-or-mp4 image use-mp4)         ; Get image link or MP4 link if desired
                        (string/split #"/")              ; Separate the URL by slashes
                        last                             ; The last item will be the file name
                        (string/split #"\.")             ; Split the file name by the period
                        last)                            ; The suffix will be the last item
        file-name (if (not (nil? (:title image)))           ; Check for usable image title
                    (:title image)                          ; Use the title property
                    (if (not (nil? (:description image)))   ; No title, check for usable description
                      (:description image)                  ; Use the description property
                      (:id image)))]                        ; No description either, use imgur id
    (str (format fmt index) "_"                             ; Build the file name, starting with index and underscore
         (shorten-and-clean file-name)                      ; Sanitize the file name
         "." file-suffix)))                                 ; Finally, add a period and the file extension

(defn- fix-album-id
  "Converts imgur album URLs to album IDs.
  \"https://imgur.com/a/abcDEF\" returns \"abcDEF\""
  [album-id]
  (let [split-id (string/split album-id #"/")]
    (get split-id (+ (.indexOf split-id "a") 1)))) ; The album ID follows the "a" item (should be last!)

(defn- get-urls-paths
  "Takes an album URL and creates a map of the image URLs
  and the resultant file paths where each image will be saved"
  [album-url use-mp4]
  (let [album (get-album (fix-album-id album-url))]         ; Extract album ID and call API to get data
    (loop [images (:images album)                           ; Get the vec of image data
           paths '[]                                        ; File paths will be appended on each iteration
           links '[]]                                       ; File URLs will be appended on each iteration
      (if (not (empty? images))
        (recur (rest images) ; Restart the loop with the remaining images and add path/link to vectors
               (conj paths (dl/create-save-path "imgur" (get-album-title album) ; Add current image destination path
                                                (get-file-name (first images) ; Determine file name for image
                                                               (-> (count (:images album)) ; Take total image count
                                                                   (- (count images)) ; Subtract remaining count from it
                                                                   (+ 1)) ; Increment it (first index = 1, not 0)
                                                               ; Figure out how large our index format should be.
                                                               ; Get the number of images and convert to string.
                                                               ; Then get the length of that string, which will
                                                               ; tell us how many leading zeros to format
                                                               ; ex. 100 -> "100" -> 3, 10 -> "10" -> 2
                                                               (str "%0" (-> (count (:images album))
                                                                             str
                                                                             count) "d")
                                                               use-mp4)))
               (conj links (link-or-mp4 (first images) use-mp4))) ; Add current image source URL
        {:urls links :paths paths}))))

(defn- gallery-to-album
  "Replaces an imgur gallery link with an imgur album link"
  [album-url]
  (string/replace album-url #"gallery" "a"))

(defn save-album
  "Creates lists of image links and destination paths,
  then executes the save operation in parallel."
  [album-url use-mp4]
  (let [dl-info (get-urls-paths (gallery-to-album album-url) use-mp4)]
    (time (dl/parallel-save-image (:paths dl-info) (:urls dl-info))))) ; execute the parallel save
