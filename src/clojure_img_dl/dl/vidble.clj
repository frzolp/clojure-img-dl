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
(ns clojure-img-dl.dl.vidble
  (:require [clojure-img-dl.util.download :as dl]
            [clojure.string :as string])
  (:use pl.danieljanus.tagsoup)
  (:gen-class))

; Unlike Imgur, Vidble offers no public API, so this forces us to
; pull in an album's HTML content and parse it to locate the names
; of the image files manually.

(defn- load-url [album-url]
  "Uses tagsoup to convert an album's HTML to nested vectors and maps"
  (parse album-url))

(defn- find-image-names
  "Parses the page's HTML as a data structure to locate image URLs"
  [raw-album]
  ; Search the structure, but as of 2-Jun-16, the thumbnail URLs are located in:
  ; <body>
  ;   <form>
  ;     <div class="container-fluid">
  ;       <div class="row-fluid">
  ;         <input id="ContentPlaceHolder1_thumbs">
  ;           "(urls)"
  ; </input></div></div></form></body>

  ; TODO: fix ugly recursion
  (loop [mytags (-> raw-album
                    (get 3)                                 ; the "body" tag
                    (get 2)                                 ; the "form" tag
                    (get 7)                                 ; the "div class=container-fluid" tag
                    (get 2))                                ; the "div class=row-fluid" tag
         result nil]                                        ; will contain the string of image names
    ; We are looking for an <input id="ContentPlaceHolder1_thumbs"> tag.
    ; Its value will be a comma-separated list of the thumbnail filenames.
    (if (not (empty? mytags))                               ; Are there tags to find?
      (if (vector? (first mytags))                          ; Is the first tag a vector?
        (if (= (str (first (first mytags))) ":input")       ; Is the first item in the tag's vec ":input"?
          (if (= (:id (get (first mytags) 1)) "ContentPlaceHolder1_thumbs") ; Have we found the right ID?
            (recur [] (:value (get (first mytags) 1)))      ; Ugly! Recur with an empty vec and the result
            (recur (rest mytags) nil))                      ; Wrong input tag, keep looking
          (recur (rest mytags) nil))                        ; Tag is not ":input", keep looking
        (recur (rest mytags) nil))                          ; Tag is not a vec, keep looking
      result)))                                             ; Return the result when finished

(defn- make-full-path
  "Creates a file path using the image host prefix, album ID, and file name"
  [album-url filename]
  ; Results in "vidble_(albumid)/(idx)_(filename).(suffix)
  (dl/create-save-path "vidble" (last (string/split album-url #"/")) filename))

(defn- correct-filename
  "Gets full-size image filename by removing the thumbnail suffix (\"_sqr\")"
  [filename]
  (str (first (string/split filename #"_")) "."             ; Gets "abcdefg" from "abcdefg_sqr.jpeg" and adds period
       (last (string/split filename #"\."))))               ; Gets "jpeg" from "abcdefg_sqr.jpeg" to make "abcdefg.jpeg"

(defn- get-urls-paths
  "Creates a map with vecs of image URLs and their respective file paths"
  [album-url]
  ; Convert the comma separated list of thumbnail files to a vec, excluding first "redd.it" item
  ; and get the number of images to determine the size of our image index format string
  (let [image-names (rest (string/split (find-image-names (load-url album-url)) #","))
        idx-format (str "%0" (count (str (count image-names))) "d")]
    (loop [filenames image-names                            ; Vec of thumnail filenames
           urls '[]                                         ; Vec of image source URLs
           paths '[]                                        ; Vec of destination filepaths
           x 1]                                             ; Image index counter
      (if (not (empty? filenames))                          ; Are there still files to process?
        (recur (rest filenames)                             ; Loop with the remaining filenames
               ; Add the URL of the full-resolution image to the urls vec
               (conj urls (str "http://vidble.com/" (correct-filename (first filenames))))
               ; Add the destination file path to the files vec
               (conj paths (make-full-path album-url (str (format idx-format x) "_" (correct-filename (first filenames)))))
               ; Bump the counter
               (inc x))
        ; Done! Return a map containing the source URL vec and the destination path vec
        {:urls urls :paths paths}))))

(defn save-album
  "Gets the image data for the Vidble album then downloads the images"
  [album-url]
  (let [dl-info (get-urls-paths album-url)]
    (time (dl/parallel-save-image (:paths dl-info) (:urls dl-info)))))
