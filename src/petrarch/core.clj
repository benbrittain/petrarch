(ns petrarch.core
  (:require [ring.middleware.reload :as reload]
            [org.httpkit.server :as http-kit]
            [ring.util.response :as resp]
            [ring.middleware.edn :as edn]
            [taoensso.sente :as sente]
            [taoensso.sente.server-adapters.http-kit :refer (sente-web-server-adapter)]
            [petrarch.db.core :as db]
            [compojure.route :as route]
            [compojure.handler :refer [site]]
            [compojure.core :refer [defroutes GET POST PUT DELETE ANY context]])
  (:gen-class))


(let [{:keys [ch-recv send-fn ajax-post-fn ajax-get-or-ws-handshake-fn connected-uids]}
      (sente/make-channel-socket! sente-web-server-adapter {})]
  (def ring-ajax-post                ajax-post-fn)
  (def ring-ajax-get-or-ws-handshake ajax-get-or-ws-handshake-fn)
  (def ch-chsk                       ch-recv) ; ChannelSocket's receive channel
  (def chsk-send!                    send-fn) ; ChannelSocket's send API fn
  (def connected-uids                connected-uids)) ; Watchable, read-only atom





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

(defn save-coords [coords]
  (doseq [point coords]
    (db/insert-point! point)))

(defroutes routes
  (GET "/" [] (resp/resource-response "index.html" {:root "public"}))
  (GET "/api/entry/" [] (get-entries-index))
  (GET "/api/entry/:id" [id] (get-entry id))
  (POST "/api/routes/" [] (fn [req]
                            (let [edn (:params req)]
                              (save-coords (:coords edn))
                              (generate-response {:text "good"}))))
  (GET "/api/routes/" [] (fn [req]
                           (let [edn (:params req)
                                 route (into [] (db/get-5000-points))]
                             (generate-response {:route route}))))
  (GET  "/chsk" req (ring-ajax-get-or-ws-handshake req))
  (POST "/chsk" req (ring-ajax-post                req))
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
