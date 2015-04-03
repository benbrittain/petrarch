(ns petrarch.core
  (:require [ring.middleware.reload :as reload]
            [org.httpkit.server :as http-kit]
            [ring.util.response :as resp]
            [ring.middleware.edn :as edn]
            [petrarch.db :as db]
            [compojure.route :as route]
            [compojure.handler :refer [site]]
            [compojure.core :refer [defroutes GET POST PUT DELETE ANY context]])
  (:gen-class))


(defn generate-response [data & [status]]
  {:status (or status 200)
   :headers {"Content-Type" "application/edn"}
   :body (pr-str data)})

(defn get-entries-index []
  (let [entries (read-string (slurp "resources/data/entries.edn"))
        entries-sans-text (into [] (map #(dissoc % :text) entries))]
    (generate-response entries-sans-text)))

(defn get-entry [id]
  (let [entries (read-string (slurp "resources/data/entries.edn"))
        entry (first (filter #(= (str (:id %)) id) entries))]
    (generate-response {:text (:text entry)})))

(defn get-routes []
  (let [routes (read-string (slurp "resources/data/routes.edn"))]
    (generate-response {:path routes})))

(defn save-coords [coords]
  (doseq [point coords]
    (db/insert-point point)))

(defroutes routes
  (GET "/" [] (resp/resource-response "index.html" {:root "public"}))
  (GET "/api/entry/" [] (get-entries-index))
  (GET "/api/entry/:id" [id] (get-entry id))
  (POST "/api/routes/" [] (fn [req]
                            (let [edn (:params req)]
                              (save-coords (:coords edn))
                              (generate-response {:text "good"}))))
  (route/resources "/")
  (route/not-found "<p>No such adventure has been taken yet</p>"))

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
