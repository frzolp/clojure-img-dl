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
(ns clojure-img-dl.util.download
  (:require [clojure.java.io :as io]
            [clojure.string :as string])
  (:import [java.io File]))

(def ^{:private true} o (Object.))

(defn- copy
  "Downloads an image from a URL and saves it to a file on disk"
  [uri file]
  (with-open [in (io/input-stream uri)     ; Input stream (from image URL)
              out (io/output-stream file)] ; Output stream (to local file)
    (io/copy in out)))

(defn- save-image [path link]
  (if (and (not (nil? path)) (not (nil? link)))
    (do
      (copy link path)
      ; Wrap the println in a locking fn to ensure each line prints one at a time.
      (locking o (println (str "Saving \"" link "\" to \"" path "\""))))))

(defn parallel-save-image
  "Fires off multiple save operations in parallel."
  [path link]
  ; Maps save-image as a future to the path and link vectors, kicking off threads in parallel
  (let [threads (doall (map #(future
                              (save-image %1 %2 )) path link))]
    ; Wrap the doall in a def to avoid printing (nil, nil) lines for each image
    (def done (doall (map deref threads)))) ; Block until all threads have finished
  nil) ; Best to return nil compared to the last defined var, "done"

(defn create-save-path
  "Creates a directory for the album, then builds the path
  where the image will be saved as \"folder/filename\""
  [host album file]
  (def path (str host "_" album "/" file)) ; Assemble the path
  (.mkdir (File. (first (string/split path #"/")))) ; Create the directory if it does not exist
  path) ; Return the path