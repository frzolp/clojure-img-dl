(defproject clojure-img-dl "1.0.0-RELEASE"
  :description "An imgur album downloader written in Clojure"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/data.json "0.2.6"]
                 [clj-http "2.1.0"]
                 [clojurewerkz/propertied "1.2.0"]]
  :main ^:skip-aot clojure-img-dl.core
  :target-path "target/%s"
  :resource-paths ["resources"]
  :profiles {:uberjar {:aot :all}})