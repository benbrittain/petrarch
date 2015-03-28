(ns ^:figwheel-always petrarch.core
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [cljs.core.async :refer [put! chan <!]]))

(enable-console-print!)

;; define your app data so that it doesn't get over-written on reload
(defonce app-state
  (atom
    {:entries
     [{:title "The flight from society" :date "3/26/15"}
      {:title "blog tech" :date "3/26/15"}]}))

(defn entry-view [entry owner]
  (reify
    om/IRender
    (render [this]
      (dom/li nil (:title entry)))))


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
      (dom/div #js {:id "the-map"
                    :style #js {:height 500}}
               nil))
    om/IDidMount
    (did-mount [_]
      (let [osm-url "http://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
            osm-attrib "Map data © OpenStreetMap contributors"]
        (doto (js/L.Map. "the-map")
          (.setView (js/L.LatLng. 0 0) 1)
          (.addLayer (js/L.TileLayer. osm-url
                                      #js {:minZoom 1, :maxZoom 19,
                                           :attribution osm-attrib})))))))

(defn page-view [data owner]
  (reify
    om/IRender
    (render [this]
      (dom/div nil
               (dom/div #js {:id "entries"}
                        (om/build entries-view data))
               (dom/div #js {:id "map"}
                        (om/build map-view data))))))
; Render root
(om/root page-view app-state
  {:target (. js/document (getElementById "app"))})
