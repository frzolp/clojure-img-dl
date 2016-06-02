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
(ns clojure-img-dl.core
  (:require [clojure-img-dl.dl.imgur :as imgur]
            [clojure-img-dl.dl.vidble :as vidble]
            [clojure.string :as string])
  (:gen-class))

(defn -main
  "Downloads albums by URL passed by command line arguments
  ex. \"lein run https://imgur.com/a/aBcDE QWertY\""
  [& args]
  (loop [albums args]
    (if (not (empty? args)) ; Do we have a non-empty argument list?
      (if (not (string/blank? (first albums))) ; Is the current argument not empty?
        (do
          (println (str "Saving \"" (first albums) "\""))
          (let [album (first albums)]
            (cond
              (true? (string/includes? album "vidble.com")) (vidble/save-album album)
              (true? (string/includes? album "imgur.com")) (imgur/save-album album)
              :else (do
                      (println (str "Unknown image host for " album "."))
                      (println "Only supports Imgur and Vidble at this time."))))
          (recur (rest albums)))
        (println "Done")) ; A blank string indicates end of argument list
      (println "Usage: <lein run | java -jar clojure-img-dl-(ver).jar> [<album_url> ...]"))) ; No args! Show usage
  (System/exit 0))       ; Explicitly exit upon completion or else completed futures will hang for 1 minute
