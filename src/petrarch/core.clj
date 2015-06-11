(ns petrarch.core
  (:require [ring.middleware.reload :as reload]
            [org.httpkit.server :as http-kit]
            [ring.util.response :as resp]
            [ring.middleware.edn :as edn]
            [ring.middleware.multipart-params :as mp]
            [ring.middleware.pratchett :refer [wrap-pratchett]]
            [clojure.java.io :as io]
            [petrarch.db.core :as db]
            [environ.core :refer [env]]
            [compojure.route :as route]
            [compojure.handler :refer [site]]
            [compojure.core :refer [defroutes GET POST PUT DELETE ANY context]])
  (:gen-class))

(def password (env :password))

(defn generate-response [data & [status]]
  {:status (or status 200)
   :headers {"Content-Type" "application/edn"}
   :body (pr-str data)})

(defn get-entries-index []
  (let [entries (db/get-entries)]
    (generate-response {:entries entries})))

(defn get-entry [id]
  (let [entry (db/get-entry id)]
    (generate-response {:text (:post (first entry))})))

(defn create-entry [title post-content location]
  (db/insert-entry! title post-content location))

(defn save-coords [coords]
  (doseq [point coords]
    (db/insert-point! point)))

(defn get-route [lat lng radius]
  (:coordinates (:route (first (db/get-route lat lng radius)))))

(defroutes routes
  (GET "/" [] (resp/resource-response "index.html" {:root "public"}))
  (GET "/api/entry/" [] (get-entries-index))
  (POST "/api/entry/" [] (fn [req]
                           (let [edn (:params req)]
                             (if (= (:password edn) password)
                               (do
                                 (create-entry (:title edn) (:post edn) (:location edn))
                                 (generate-response {:text "good"}))
                               (generate-response {:text "bad passphrase"} 418)))))
  (GET "/api/entry/:id" [id] (get-entry id))
  (POST "/api/routes/" [] (fn [req]
                            (let [edn (:params req)]
                             (if (= (:password edn) password)
                               (do
                                 (save-coords (:coords edn))
                                 (generate-response {:text "good"}))
                               (generate-response {:text "bad passphrase"} 418)))))
  (GET "/api/routes" [] (fn [req]
                           (let [edn (:params req) lat (:lat edn)
                                 lng (:long edn) radius (:radius edn)]
                             (generate-response {:text "emergency-hotfix"} 440))))
  (mp/wrap-multipart-params
    (POST "/api/image/" [] (fn [req]
                             (let [edn (:params req)]
                               (if (= (:password edn) password)
                                 (do (io/copy (io/file (:tempfile (:file edn)))
                                              (io/file (str "resources/public/images/" (:filename (:file edn)))))
                                     (generate-response {:text "good"}))
                                 (generate-response {:text "bad passphrase"} 418))))))
    (route/resources "/")
  (route/not-found "<p>No such adventure has been taken yet</p>"))

; App Setup

(def app
  (-> routes
      edn/wrap-edn-params
      wrap-pratchett))

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
