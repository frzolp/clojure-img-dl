(defproject clojure-img-dl "0.2.5"
  :description "An image album downloader written in Clojure"
  :url "https://github.com/frzolp/clojure-img-dl"
  :license {:name "GNU General Public License"
            :url "http://www.gnu.org/licenses/gpl.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/data.json "0.2.6"]
                 [clj-http "2.1.0"]
                 [clojurewerkz/propertied "1.2.0"]
                 [clj-tagsoup "0.3.0" :exclusions [org.clojure/clojure]]]
  :main ^:skip-aot clojure-img-dl.core
  :target-path "target/%s"
  :resource-paths ["resources"]
  :profiles {:uberjar {:aot :all}})
