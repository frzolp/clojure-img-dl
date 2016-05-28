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
            [clojurewerkz.propertied.properties :as p])
  (:import [java.io File])
  (:gen-class))

; Client ID needed to interact with imgur's API
(def auth-key (str "Client-ID " (-> (io/resource "apikey.properties")
                                    (p/load-from)
                                    (p/properties->map true)
                                    (:apikey))))

; Base URL for API calls
(def api-base "https://api.imgur.com/3/")

; Semaphore used to keep console output tidy
(def o (Object.))

(defn copy
  "Downloads an image from a URL and saves it to a file on disk"
  [uri file]
  (with-open [in (io/input-stream uri)     ; Input stream (from image URL)
              out (io/output-stream file)] ; Output stream (to local file)
    (io/copy in out)))

(defn get-album
  "Request an album's information from imgur and convert the JSON to a map"
  [album-id]
  (-> (str api-base "/album/" album-id) ; Assemble the API version of the URL
      (client/get {:headers {:authorization auth-key}}) ; Call the API endpoint with authorization key
      :body ; Get the body of the response
      (json/read-str :key-fn keyword) ; Parse the body's JSON into a map
      :data)) ; Return the data field

(defn get-album-title
  "Gets a title for the album, using the title field if available, falls back to album ID"
  [album]
  (if (not (nil? (:title album))) ; Does the album have a title?
    (string/trim (string/replace (if (> (count (:title album)) 30) ; If the title is over 30 chars, trim it
                                   (subs (:title album) 0 30)
                                   (:title album))
                                 #"[<>:\"/\\\|\?\*\+\-\^\$\(\)\.,;\%\r\n]" "")) ; Strip out invalid character and return
    (:id album))) ; Otherwise, use the raw imgur album ID

(defn get-file-name
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

(defn create-save-path
  "Creates a directory for the album, then builds the path
  where the image will be saved as \"folder/filename\""
  [album file]
  (def path (str "imgur_" album "/" file)) ; Assemble the path
  (.mkdir (File. (first (string/split path #"/")))) ; Create the directory if it does not exist
  path) ; Return the path

(defn save-image [path link]
  (if (and (not (nil? path)) (not (nil? link)))
    (do
      (copy link path)
      ; Wrap the println in a locking fn to ensure each line prints one at a time.
      (locking o (println (str "Saving \"" link "\" to \"" path "\""))))))

(defn fix-album-id
  "Converts imgur album URLs to album IDs.
  \"https://imgur.com/a/abcDEF\" returns \"abcDEF\",
   while \"abcDEF\" returns \"abcDEF\""
  [album-id]
  (let [split-id (string/split album-id #"/")]
    (if (> (count split-id) 1)                     ; A split URL will produce a list of more than one item
      (get split-id (+ (.indexOf split-id "a") 1)) ; The album ID follows the "a" item
      (first split-id))))                          ; A split album ID will only have one item

(defn parallel-save-image
  "Fires off multiple save operations in parallel."
  [path link]
  ; Maps save-image as a future to the path and link vectors, kicking off threads in parallel
  (let [threads (doall (map #(future
                              (save-image %1 %2 )) path link))]
    ; Wrap the doall in a def to avoid printing (nil, nil) lines for each image
    (def done (doall (map deref threads)))) ; Block until all threads have finished
  nil) ; Best to return nil compared to the last defined var, "done"

(defn parallel-save-album
  "Creates lists of image sources and destinations then executes a parallel download."
  [album-id]
  (let [album (get-album (fix-album-id album-id))]
    (loop [images (:images album)
           paths '[]  ; File paths will be appended on each iteration
           links '[]] ; File URLs will be appended on each iteration
      (if (not (empty? images))
        (recur (rest images) ; Restart the loop with the remaining images and add path/link to vectors
               (conj paths (create-save-path (get-album-title album) ; Add current image destination path
                                             (get-file-name (first images)
                                                            (+ 1 (- (count (:images album))
                                                                    (count images)))
                                                            (str "%0" (count (str (count (:images album)))) "d"))))
               (conj links (:link (first images)))) ; Add current image source URL
        (time (parallel-save-image paths links)))))) ; If no more images, execute the parallel save
