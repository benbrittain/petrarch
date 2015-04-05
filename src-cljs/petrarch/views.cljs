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
                                   (om/build map/map-view data {:init-state {:entries entries
                                                                             :routes routes}}))))))))
