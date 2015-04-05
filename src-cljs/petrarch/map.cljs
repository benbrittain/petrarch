(ns ^:figwheel-always petrarch.map
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [taoensso.sente :as sente]
            [petrarch.connection :as conn]
            [cljs.core.async :refer [put! chan <! >!]]))

(defn entry->marker [entry]
  (js/L.marker. (js/L.LatLng. (:latitude entry) (:longitude entry))))

(defn split-routes [points & [routes]]
  (if (nil? routes)
    (split-routes (rest points) [[(first points)]])
    (if (empty? points)
      routes
      (if (> (.distanceTo (first points) (last (last routes))) 50)
        (split-routes (rest points) (conj routes [(first points)]))
        (split-routes (rest points)
                      (conj (drop-last routes) (conj (last routes) (first points))))))))

(defn add-route [the-map route]
  (let [latlngs (map (fn [a] {:lat (first (:coordinates (:point a)))
                              :long (second (:coordinates (:point a)))}) route)
        points (map #(js/L.LatLng. (:lat %) (:long %)) latlngs)]
    (go
      (loop [points (rest points) acc [(first points)]]
        (if (empty? points)
          (let [polyline (js/L.polyline. (clj->js acc))]
            (.addTo polyline the-map))
          (if (> (.distanceTo (first points) (last acc)) 30)
            (let [polyline (js/L.polyline. (clj->js acc))]
              (.addTo polyline the-map)
              (recur (rest (rest points)) [(first (rest points))]))
            (recur (rest points) (conj acc (first points)))))))))

(defn get-points [routes-chan & [center-point radius]]
  (conn/chsk-send! [:petrarch/get-routes {:center-point center-point
                                          :radius radius}] 5000
                   (fn [edn-reply]
                     (if (sente/cb-success? edn-reply)
                       (put! routes-chan (:routes edn-reply))
                       (println "Error!")))))

(defn route-update [routes-chan latlng bounds]
  (let [radius (.distanceTo latlng (.getNorthEast bounds))
        center-point {:lat (.-lat latlng) :long (.-lng latlng)}]
      (get-points routes-chan center-point radius)))

(defn map-view [data owner]
  (reify
    om/IRenderState
    (render-state [this {:keys [entries routes]}]
      (dom/div #js {:id "the-map"} "Rendering Map..."))
    om/IWillMount
    (will-mount [_]
      (let [routes (om/get-state owner :routes)]
        (get-points routes)))
    om/IDidMount
    (did-mount [_]
      (let [entries (om/get-state owner :entries)
            routes (om/get-state owner :routes)
            the-map (js/L.mapbox.map. "the-map" "bbrittain.lj6l79gh")]
        (doto the-map
;          (.setView (js/L.LatLng. 13.75 100.0) 8)
          (.setView (js/L.LatLng. 42.36 -71.09) 10)
          (.on "moveend" #(route-update routes
                                        (.getCenter (.-target %))
                                        (.getBounds (.-target %)))))
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
