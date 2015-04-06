(ns ^:figwheel-always petrarch.views
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [petrarch.map :as map]
            [petrarch.connection :as conn]
            [markdown.core :as markdown]
            [secretary.core :as secretary :refer-macros [defroute]]
            [ajax.core :refer [GET POST]]
            [cljs.core.async :refer [put! chan <! >!]]))

(defn read-view [entry owner]
  (reify
    om/IDidMount
    (did-mount [_]
      (let [entries (om/get-state owner :entries)]
        (GET (str "api/entry/" (:id entry))
             {:handler (fn [response]
                         (println response)
                         (om/transact! entry :text (fn [_] "oh hey")))
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


(defn send-post [owner]
    (let [password (om/get-node owner "password")
          post-text (om/get-node owner "post")
          title (om/get-node owner "title")
          submit-post (fn [position]
                        (let [longitude (.-longitude js/position.coords)
                              latitude (.-latitude js/position.coords)]
                          (POST "api/entry/" {:params {:password (.-value password)
                                                       :title (.-value title)
                                                       :post (.-value post-text)
                                                       :location {:lat latitude
                                                                  :long longitude}}
                                              :format :edn
                                              :handler (fn [response]
                                                         (println response))
                                              :error-handler (fn [error]
                                                               (println error))})))]
      (.getCurrentPosition js/navigator.geolocation submit-post)))

(defn new-entry-view [data owner]
  (reify
    om/IRender
    (render [this]
      (dom/div nil
               (dom/h2 nil "Submit a post")
               (dom/input #js {:type "text" :ref "password"})
               (dom/input #js {:type "text" :ref "title"})
               (dom/input #js {:type "text" :ref "post"})
               (dom/button #js {:onClick #(send-post owner)} "Submit post")))))

(defn page-view [data owner]
  (reify
    om/IInitState
    (init-state [_]
      {:entries (chan)
       :routes (chan)})
    om/IWillMount
    (will-mount [_]
      (let [entries-chan (om/get-state owner :entries)]
        (GET "api/entry/"
             {:handler (fn [response]
                         (let [entries (into [] (:entries response))]
                           (doall
                             (map #(put! entries-chan %) entries))
                           (om/transact! data :entries (fn [_] entries))))
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
                            :new-entry (dom/div #js {:className "entries new-entry"}
                                                (om/build new-entry-view {:init-state {}}))
                            (dom/div #js {:className "blah"} "how did you get here..."))
                          (dom/div #js {:className "map"}
                                   (om/build map/map-view data {:init-state {:entries entries
                                                                             :routes routes}}))))))))
