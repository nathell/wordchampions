(ns gridlock.core
  (:require
    [gridlock.events :as events]
    [gridlock.subs :as subs]
    [gridlock.views :as views]
    [re-frame.core :refer [dispatch-sync]]
    [reagent.core :as r]))

(defn init []
  (dispatch-sync [:init])
  (r/render [views/root] (.getElementById js/document "app")))

(js/document.addEventListener "DOMContentLoaded" init)
