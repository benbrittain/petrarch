(ns ^:figwheel-always petrarch.map
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [cljs.core.async :refer [put! chan <! >!]]))

(defn entry->marker [entry]
  (js/L.marker. (js/L.LatLng. (:latitude entry) (:longitude entry))))

(defn add-route [the-map route]
  (let [latlngs (map (fn [a] {:lat (second (:coordinates (:point a))) :long (first (:coordinates (:point a)))}) route)
        points (map #(js/L.LatLng. (:lat %) (:long %)) latlngs)
        polyline (js/L.polyline. (clj->js points))]
    (.addTo polyline the-map)
    (doto the-map
      (.fitBounds (.getBounds polyline)))))

(defn map-view [data owner]
  (reify
    om/IRenderState
    (render-state [this {:keys [entries routes]}]
      (dom/div #js {:id "the-map"} "Rendering Map..."))
    om/IWillMount
    (will-mount [_]
      (let [routes (om/get-state owner :routes)]))
;        (GET "api/routes/"
;             {:handler (fn [response]
;                         (put! routes (:route response)))
;              :error-handler (fn [error] (println error))})))
    om/IDidMount
    (did-mount [_]
      (let [entries (om/get-state owner :entries)
            routes (om/get-state owner :routes)
            the-map (js/L.mapbox.map. "the-map" "bbrittain.lj6l79gh")]
        (doto the-map
          (.setView (js/L.LatLng. 13.75 100.0) 8)
          (.on "move" #(js/console.log (.getBounds (.-target %)))))
        (go (loop []
              (let [entry (<! entries)]
                (-> (entry->marker entry)
                    (.addTo the-map)
                    (.bindPopup (str "<a href=#/entry/" (:id entry) " >" (:title entry) "</a>")))
                (recur))))
        (go (loop []
              (let [route (<! routes)]
                (add-route the-map route)
                (recur))))))))
