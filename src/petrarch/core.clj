(ns petrarch.core
  (:require [ring.middleware.reload :as reload]
            [org.httpkit.server :as http-kit]
            [ring.util.response :as resp]
            [ring.middleware.edn :as edn]
            [compojure.route :as route]
            [compojure.handler :refer [site]]
            [compojure.core :refer [defroutes GET POST DELETE ANY context]])
  (:gen-class))


(defn generate-response [data & [status]]
  {:status (or status 200)
   :headers {"Content-Type" "application/edn"}
   :body (pr-str data)})

(defn get-entries-index []
  (generate-response [{:id 0 :title "test" :date "03/28/2015" :latitude 13.75 :longitude 100}
                      {:id 1 :title "The Blog" :date "03/28/2015" :latitude 13.75 :longitude 100}
                      {:id 2 :title "What am I bringing?" :date "03/29/2015" :latitude 13.75 :longitude 100}]))

(defn get-entry [id]
  (generate-response {:text (str "#test
woah, so great: " id)}))

(defroutes routes
  (GET "/" [] (resp/resource-response "index.html" {:root "public"}))
  (GET "/api/entry/" [] (get-entries-index))
  (GET "/api/entry/:id" [id] (get-entry id))
  (route/resources "/")
  (route/not-found "<p>No such adventure has been taken yet</p>")) ;; all other, return 404



; App Setup

(def app
  (-> routes
      edn/wrap-edn-params))

(defonce server
         (atom nil))

(defn parse-port [args]
  (if-let [port (first args)]
    (Integer/parseInt port)
    3000))

(defn- start-server [port args]
  (reset! server
          (http-kit/run-server
            (reload/wrap-reload (site #'app)) {:port port})))

(defn- stop-server []
  (@server))

(defn -main [& args]
  (let [port (parse-port args)]
    (start-server port args)
    (println "server started on port:" port)))
