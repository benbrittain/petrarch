(ns petrarch.core
  (:require [ring.middleware.reload :as reload]
            [org.httpkit.server :as http-kit]
            [ring.util.response :as resp]
            [ring.middleware.edn :as edn]
            [ring.middleware.multipart-params :as mp]
            [clojure.java.io :as io]
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

(defmulti event-msg-handler :id) ; Dispatch on event-id

(defn event-msg-handler* [{:as ev-msg :keys [id ?data event]}]
  (println "Event: " event)
  (event-msg-handler ev-msg))

(do
  (defmethod event-msg-handler :default
    [{:as ev-msg :keys [event id ?data ring-req ?reply-fn send-fn]}]
    (let [session (:session ring-req)
          uid     (:uid session)]
      (println "Unhandled event: " event)
      (when ?reply-fn
        (?reply-fn {:umatched-event-as-echoed-from-from-server event}))))

  (defmethod event-msg-handler :petrarch/get-routes
    [{:as ev-msg :keys [event id ?data ring-req ?reply-fn send-fn]}]
    (let [session (:session ring-req)
          uid     (:uid session)]
      (if (nil? (:center-point ?data))
        (?reply-fn {:routes (into [] (db/get-1000-points))})
        (?reply-fn {:routes (into []
                              (db/get-points-in-region
                                (:center-point ?data) (:radius ?data)))})))))

(sente/start-chsk-router! ch-chsk event-msg-handler*)

(defn generate-response [data & [status]]
  {:status (or status 200)
   :headers {"Content-Type" "application/edn"}
   :body (pr-str data)})

(defn get-entries-index []
  (let [entries (db/get-entries)]
    (println entries)
    (generate-response {:entries entries})))

(defn get-entry [id]
  (let [entry (db/get-entry id)]
    (generate-response {:text (:post (first entry))})))

(defn create-entry [title post-content location]
  (db/insert-entry! title post-content location))

(defn save-coords [coords]
  (doseq [point coords]
    (db/insert-point! point)))

(defn save-file [username file]
  (println username)
  (println file))

(defroutes routes
  (GET "/" [] (resp/resource-response "index.html" {:root "public"}))
  (GET "/api/entry/" [] (get-entries-index))
  (POST "/api/entry/" [] (fn [req]
                           (let [edn (:params req)]
                             (create-entry (:title edn) (:post edn) (:location edn))
                             (generate-response {:text "good"}))))
  (GET "/api/entry/:id" [id] (get-entry id))
  (POST "/api/routes/" [] (fn [req]
                            (let [edn (:params req)]
                              (save-coords (:coords edn))
                              (generate-response {:text "good"}))))
  (GET "/api/routes/" [] (fn [req]
                           (let [edn (:params req)
                                 route (into [] (db/get-1000-points))]
                             (generate-response {:route route}))))
  (mp/wrap-multipart-params
    (POST "/api/image/" [] (fn [req]
                             (let [edn (:params req)]
                               (println edn)
                               (io/copy (io/file (:tempfile (:file edn)))
                                        (io/file (str "resources/public/images/" (:filename (:file edn)))))
                               (generate-response {:text "good"})))))
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
