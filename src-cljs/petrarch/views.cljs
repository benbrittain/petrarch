(ns ^:figwheel-always petrarch.views
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [petrarch.map :as map]
            [markdown.core :as markdown]
            [secretary.core :as secretary :refer-macros [defroute]]
            [ajax.core :refer [GET POST]]
            [cljs.core.async :refer [put! chan <! >!]]))

(defn ts->date [ts]
  (println (clj->js ts))
  (dom/p nil
         (.toLocaleString (clj->js ts))))

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
                          (dom/h2 nil (ts->date (:timestamp entry))))
                 (dom/div #js {:id "markdown" :dangerouslySetInnerHTML #js {:__html text}}
                          nil))))))

(defn entry-view [entry owner]
  (reify
    om/IRender
    (render [this]
              (dom/a #js {:href (str "#/entry/" (:id entry))}
                     (dom/div #js {:className "entry"}
                              (dom/span #js {:className "titlee"}
                                        (:title entry))
                              (dom/span #js {:className "date"}
                                        (ts->date (:timestamp entry))))))))
(defn entries-view [data owner]
  (reify
    om/IRender
    (render [this]
      (dom/div nil
        (apply dom/div nil
               (om/build-all entry-view
                 (reverse (sort-by #(:id %) (:entries data)))))))))


(defn send-file [owner]
  (let [password (.-value (om/get-node owner "password"))
        filename (.-value (om/get-node owner "filename"))
        image (.item (.-files (.getElementById js/document "image-upload")) 0)
        form-data (doto
                    (js/FormData.)
                    (.append "password" password)
                    (.append "file" image filename))]
    (POST "api/image/" {:params form-data
                        :response-format :edn})))


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
    om/IDidMount
    (did-mount [_]
      (let [editor (js/EpicEditor. #js {:theme #js {:base "/../js/epiceditor/themes/base/epiceditor.css"
                                                    :preview "/../js/epiceditor/themes/preview/preview-dark.css"
                                                    :editor "/../js/epiceditor/themes/editor/epic-dark.css"}})]
        (doto editor
            (.load))))
    om/IRender
    (render [this]
      (dom/div nil
        (dom/div nil
                 (dom/h2 nil "Submit a post")
                 (dom/div nil
                         (dom/input #js {:type "text" :ref "password" :placeholder "password"}))
                 (dom/div nil
                         (dom/input #js {:type "text" :ref "title" :placeholder "title" }))
                 (dom/div #js {:id "epiceditor"}
                          (dom/textarea #js {:style #js {:width 300 :height 300}
                                             :id "post" :type "text" :ref "post"}))
                 (dom/div nil
                         (dom/button #js {:onClick #(send-post owner)} "Submit post")))
               (dom/div nil
          (dom/input #js {:type "text" :ref "filename" :placeholder "file name"})
          (dom/input #js {:type "file" :id "image-upload"})
          (dom/button #js {:onClick #(send-file owner)} "Upload Image"))))))

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
                                 (dom/h1 #js {:className "title"}
                                         "\"Tourist, means Idiot.\""))
                          (dom/h1 #js {:className "subtitle"}
                                  "Ben Brittain's meanderings through Indochina"))
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
                                                                             :routes routes}})))
                 (dom/div #js {:className "footer"}
                          (dom/p nil
                                 "© Ben Brittain. Shared under the "
                          (dom/a #js {:href "http://creativecommons.org/licenses/by/4.0/"}
                                 "Creative Commons Attribution 4.0 International License")
                                 ". "
                          (dom/a #js {:href "https://github.com/cavedweller/petrarch"}
                                 "Source code")
                                 " licensed under "
                          (dom/a #js {:href "https://www.gnu.org/licenses/gpl-3.0.txt"}
                                 "GNU GPLv3") ".")))))))
