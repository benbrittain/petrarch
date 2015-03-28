(ns ^:figwheel-always petrarch.core
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [markdown.core :as markdown]
            [secretary.core :as secretary :refer-macros [defroute]]
            [ajax.core :refer [GET POST]]
            [cljs.core.async :refer [put! chan <!]]
            [goog.events :as events]
            [goog.history.EventType :as EventType])
  (:import goog.History))

(enable-console-print!)

;; define your app data so that it doesn't get over-written on reload
(defonce app-state
  (atom
    {:entries []
    :view :entries
    :entry 0}))

(defn read-view [entry owner]
  (reify
    om/IWillMount
    (will-mount [_]
      (GET (str "api/entry/" (:id entry)) {:handler (fn [response]
                                                      (om/transact! entry :text (fn [_] (:text response))))
                                           :error-handler (fn [error] (println error))}))
    om/IRender
    (render [this]
      (let [text (if (= (:text entry) nil) "text is empty"
                   (markdown/md->html (:text entry)))]
        (dom/div #js {:className "read"}
                 (dom/div #js {:id "title" :style #js {:border "1px black solid"}}
                          (dom/h1 nil (:title entry))
                          (dom/h2 nil (:date entry)))
                 (dom/div #js {:dangerouslySetInnerHTML #js {:__html text}}
                          nil))))))

(defn entry-view [entry owner]
  (reify
    om/IRender
    (render [this]
      (dom/li #js {:className "entry"}
              (dom/a #js {:href (str "#/entry/" (:id entry))}
                     (:title entry))))))

(defn entries-view [data owner]
  (reify
    om/IRender
    (render [this]
      (dom/div nil
               (dom/h2 nil "Entries")
               (apply dom/ul nil
                      (om/build-all entry-view (:entries data)))))))

(defn map-view [_ owner]
  (reify
    om/IRender
    (render [_]
      (dom/div #js {:id "the-map"}
               nil))
    om/IDidMount
    (did-mount [_]
      (let [osm-url "http://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
            osm-attrib "Map data © OpenStreetMap contributors"]
        (doto (js/L.Map. "the-map")
          (.setView (js/L.LatLng. 13.75 100.0) 8)
          (.addLayer (js/L.TileLayer. osm-url
                                      #js {:minZoom 1, :maxZoom 19,
                                           :attribution osm-attrib})))))))

(defn page-view [data owner]
  (reify
    om/IWillMount
    (will-mount [_]
      (GET "api/entry/" {:handler (fn [response]
                                 (om/transact! data :entries (fn [_] response)))
                      :error-handler (fn [error]
                                       (println error))}))
    om/IRender
    (render [this]
      (let [view (:view data)
            entry-id (:entry data)
            entry (first (seq (filter #(= (str (:id %)) entry-id) (:entries data))))]
        (dom/div #js {:className "wrapper"}
                 (condp = view
                   :entry (dom/div #js {:className "entry"}
                                   (om/build read-view entry))
                   :entries (dom/div #js {:className "entries"}
                                     (om/build entries-view data))
                   (dom/div #js {:className "blah"} nil))
                 (dom/div #js {:className "map"}
                          (om/build map-view data)))))))

; Render root
(defroute "/" [] (swap! app-state assoc :view :entries))
(defroute "/entry/:entry-id" [entry-id] (swap! app-state assoc :view :entry :entry entry-id))

(om/root page-view app-state
  {:target (. js/document (getElementById "app"))})


;; History configuration
(let [history (History.)
      navigation EventType/NAVIGATE]
  (goog.events/listen history
                      navigation
                      #(-> % .-token secretary/dispatch!))
  (doto history (.setEnabled true)))

(secretary/set-config! :prefix "#")
(secretary/dispatch! "/")
