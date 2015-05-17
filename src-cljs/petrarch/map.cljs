(ns ^:figwheel-always petrarch.map
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [ajax.core :refer [GET]]
            [cljs.core.async :refer [put! chan <! >!]]))

(defn entry->marker [entry]
  (let [latitude  (first (:coordinates (:point entry)))
        longitude (second (:coordinates (:point entry)))]
  (js/L.marker. (js/L.LatLng. latitude longitude))))


(defn remove-routes [the-map]
  (doall
    (map #(let [layer (aget (.-_layers the-map) %)]
            (if (= (.-_path layer) js/undefined) nil
              (.removeLayer the-map layer)))
         (js->clj (js/Object.keys (.-_layers the-map))))))

(defn add-route [the-map route]
  (let [latlngs (map (fn [a] {:lat (first a)
                              :long (second a)}) route)
        points (map #(js/L.LatLng. (:lat %) (:long %)) latlngs)]
    (.addTo (js/L.polyline. (clj->js points)
                            #js {:color "#AA0000" :clickable false}) the-map)))

(defn get-routes [routes-chan center-point radius]
  (GET (str "api/routes?lat=" (:lat center-point)
                      "&long=" (:long center-point)
                      "&radius=" radius)
       {:handler (fn [response]
                   (put! routes-chan (:route response)))
        :error-handler (fn [error] (println error))}))

(defn route-update [routes-chan latlng bounds]
  (let [radius (.distanceTo latlng (.getNorthEast bounds))
        center-point {:lat (.-lat latlng) :long (.-lng latlng)}]
    (get-routes routes-chan center-point radius)))

(defn map-view [data owner]
  (reify
    om/IRenderState
    (render-state [this {:keys [entries routes]}]
      (dom/div #js {:id "the-map"} "Rendering Map..."))
    om/IDidMount
    (did-mount [_]
      (let [entries (om/get-state owner :entries)
            routes (om/get-state owner :routes)
            the-map (js/L.mapbox.map. "the-map"
                                      "bbrittain.400e7102"
                                      #js {:maxZoom 12})]
        (doto the-map
          (.setView (js/L.LatLng. 13.75 102.0) 7)
          (.on "zoomend" #(remove-routes the-map))
          (.on "moveend" #(route-update routes
                                        (.getCenter (.-target %))
                                        (.getBounds (.-target %)))))
        (route-update routes (.getCenter the-map)
                             (.getBounds the-map))
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
