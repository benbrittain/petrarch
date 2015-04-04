(ns ^:figwheel-always petrarch.core
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [markdown.core :as markdown]
            [secretary.core :as secretary :refer-macros [defroute]]
            [taoensso.sente  :as sente :refer (cb-success?)]
            [ajax.core :refer [GET POST]]
            [cljs.core.async :refer [put! chan <! >!]]
            [petrarch.views :as views]
            [petrarch.connection :as connection]
            [goog.events :as events]
            [goog.history.EventType :as EventType])
  (:import goog.History))

(enable-console-print!)

;; Define app state
(defonce app-state
  (atom
    {:entries []
     :view :entries
     :entry 0}))

;; Initialize connection with backend

(connection/initialize)


; Render root
(defroute "/" [] (swap! app-state assoc :view :entries))
(defroute "/entry/:entry-id" [entry-id] (swap! app-state assoc :view :entry :entry entry-id))

(om/root views/page-view app-state
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

