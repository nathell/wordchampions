(ns kobrem.core
  (:require [reagent.core :as reagent]
            [re-frame.core :refer [reg-event-db reg-sub dispatch dispatch-sync subscribe]]))

(enable-console-print!)

(reg-event-db
 :init
 (fn [_ _]
   {:counter 0}))

(reg-event-db
 :inc
 (fn [db _]
   (update-in db [:counter] inc)))

(reg-sub
 :counter
 (fn [db _]
   (:counter db)))

(defn root []
  (let [counter (subscribe [:counter])]
    [:div
     [:h1 (str "Hello, world! " @counter)]
     [:button {:on-click #(dispatch [:inc])} "Bump"]]))

(defn init []
  (dispatch-sync [:init])
  (reagent/render [root] (.getElementById js/document "app")))

(js/document.addEventListener "DOMContentLoaded" init)
