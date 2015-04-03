(defproject petrarch "0.0.1"
  :description "travelogue software"
  :url "https://travel.benbrittain.com"
  :license {:name "GNU GPLv3"
            :url "https://www.gnu.org/licenses/gpl-3.0.txt"}

  :uberjar-name "petrarch.jar"
  :main petrarch.core
  :jvm-opts ["-server"]

  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/clojurescript "0.0-2850"]
                 [figwheel "0.2.5-SNAPSHOT"]
                 [org.clojure/core.async "0.1.346.0-17112a-alpha"]
                 [org.omcljs/om "0.8.8"]
                 [secretary "1.2.2"]
                 [markdown-clj "0.9.65"]
                 [cljs-ajax "0.3.10"]
; Clojure libs
                 [yesql "0.4.0"]
                 [org.postgresql/postgresql "9.3-1102-jdbc41"]
                 [ragtime "0.3.6"]
                 [clj-time "0.9.0"]
                 [fogus/ring-edn "0.2.0"]
                 [http-kit "2.1.18"]
                 [lib-noir "0.8.7"]
                 [compojure "1.1.8"]
                 [ring-server "0.3.1"]]

  :plugins [[lein-cljsbuild "1.0.4"]
            [lein-figwheel "0.2.5-SNAPSHOT"]
            [ragtime/ragtime.lein "0.3.6"]]

  :source-paths ["src" "src-cljs"]

  :clean-targets ^{:protect false} ["resources/public/js/compiled"]

  :cljsbuild {
    :builds [{:id "dev"
              :source-paths ["src-cljs" "dev_src"]
              :compiler {:output-to "resources/public/js/compiled/petrarch.js"
                         :output-dir "resources/public/js/compiled/out"
                         :optimizations :none
                         :main petrarch.dev
                         :asset-path "js/compiled/out"
                         :source-map true
                         :source-map-timestamp true
                         :cache-analysis true }}
             {:id "min"
              :source-paths ["src-cljs"]
              :compiler {:output-to "resources/public/js/compiled/petrarch.js"
                         :main petrarch.core
                         :optimizations :advanced
                         :pretty-print false}}]}

  :figwheel {:http-server-root "public"
             :server-port 3449
             :css-dirs ["resources/public/css"]}

  :ragtime {:migrations ragtime.sql.files/migrations
            :database "jdbc:postgresql://localhost/petrarch?user=dev"})
