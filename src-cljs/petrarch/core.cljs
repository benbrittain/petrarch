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
    om/IDidMount
    (did-mount [_]
      (let [entries (om/get-state owner :entries)]
        (GET (str "api/entry/" (:id entry))
             {:handler (fn [response]
                         (om/transact! entry :text (fn [_] (:text response))))
              :error-handler (fn [error] (println error))})))
    om/IRender
    (render [this]
      (let [text (if (= (:text entry) nil)
                   "no entry"
                   (markdown/md->html (:text entry)))]
        (dom/div #js {:className "read"}
                 (dom/div #js {:id "title"}
                          (dom/h1 nil (:title entry))
                          (dom/h2 nil (:date entry)))
                 (dom/div #js {:id "markdown" :dangerouslySetInnerHTML #js {:__html text}}
                          nil))))))

(defn entry-view [entry owner]
  (reify
    om/IRender
    (render [this]
              (dom/a #js {:href (str "#/entry/" (:id entry))}
                     (dom/div #js {:className "entry"}
                              (:title entry))))))

(defn entries-view [data owner]
  (reify
    om/IRender
    (render [this]
      (dom/div nil
               (apply dom/div nil
                      (om/build-all entry-view
                                    (reverse (sort-by #(:id %) (:entries data)))))))))

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
      (dom/div #js {:id "the-map"} "test!"))
    om/IWillMount
    (will-mount [_]
      (let [routes (om/get-state owner :routes)]
        (GET "api/routes/"
             {:handler (fn [response]
                         (put! routes (:route response)))
              :error-handler (fn [error] (println error))})))
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

(defn page-view [data owner]
  (reify
    om/IInitState
    (init-state [_]
      {:entries (chan)
       :routes (chan)})
    om/IWillMount
    (will-mount [_]
      (let [entries (om/get-state owner :entries)]
        (GET "api/entry/"
             {:handler (fn [response]
                         (doall
                           (map #(put! entries %) response))
                         (om/transact! data :entries (fn [_] response)))
              :error-handler (fn [error]
                               (println error))})))
    om/IRenderState
    (render-state [this {:keys [entries routes]}]
      (let [view (:view data)
            entry-id (:entry data)
            entry (first (seq (filter #(= (str (:id %)) entry-id) (:entries data))))]
        (dom/div #js {:id "app"}
                 (dom/div #js {:className "header"}
                          (dom/a #js {:href "#/"}
                                 (dom/h1 nil "Wandering through Indochina")))
                 (dom/div #js {:className "wrapper"}
                          (condp = view
                            :entry (dom/div #js {:className "entries"}
                                            (om/build read-view entry))
                            :entries (dom/div #js {:className "entries"}
                                              (om/build entries-view data {:init-state {:entries entries
                                                                                        :routes routes}}))
                            (dom/div #js {:className "blah"} nil))
                          (dom/div #js {:className "map"}
                                   (om/build map-view data {:init-state {:entries entries
                                                                         :routes routes}}))))))))

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
