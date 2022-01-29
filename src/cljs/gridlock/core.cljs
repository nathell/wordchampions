(ns gridlock.core
  (:require
    [gridlock.events :as events]
    [gridlock.subs :as subs]
    [gridlock.views :as views]
    [re-frame.core :refer [dispatch-sync]]
    [reagent.dom :as rdom]))

(defn ^:dev/after-load render-root []
  (rdom/render [views/root] (.getElementById js/document "app")))

(defn init []
  (dispatch-sync [:init])
  (render-root))

(js/document.addEventListener "DOMContentLoaded" init)
