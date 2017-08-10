(ns kobrem.core
  (:require [reagent.core :as r]
            [re-frame.core :refer [reg-event-db reg-sub dispatch dispatch-sync subscribe]]
            [kobrem.mui :as mui]
            [clojure.string :as string]))

(enable-console-print!)

(reg-event-db
 :init
 (fn [_ _]
   {:zag [{:horiz ["rabat" "akord" "marla"]
           :vert ["kakao" "obora" "marla"]
           :word "barkarola"}]}))

(reg-event-db
 :inc
 (fn [db _]
   (update-in db [:counter] inc)))

(reg-sub
 :zag
 (fn [db _]
   (:zag db)))

(def size 45)

(defn blank []
  [:div {:style {:display "inline-block"
                 :width size :height size
                 :margin-top -1 :margin-left -1
                 :border :none
                 :font-weight "bold"
                 :font-size (str (-> size (* 3) (/ 4)) "px")
                 :line-height (str size "px")
                 :vertical-align "middle"
                 :text-align "center"}}])

(defn mouse-move [state event]
  (.preventDefault event)
  (let [delta-x (- (.-pageX event) (:origin-x @state))
        delta-y (- (.-pageY event) (:origin-y @state))
        distance (+ (js/Math.abs delta-x) (js/Math.abs delta-y))]
    (when (and (not (:dragging @state)) (> distance 3))
      (swap! state assoc :dragging true))
    (when (:dragging @state)
      (swap! state assoc
             :left (+ (:element-x @state) delta-x js/document.body.scrollLeft)
             :top (+ (:element-y @state) delta-y js/document.body.scrollTop)))))

(defn mouse-up [state event]
  (js/document.removeEventListener "mousemove" (:move-handler @state))
  (js/document.removeEventListener "mouseup" (:up-handler @state))
  (when (:dragging @state)
    (swap! state assoc :dragging false)))

(defn letter [c]
  (let [blank? (= (str c) " ")
        hover (r/atom false)
        drag-state (r/atom {:dragging false})]
    (r/create-class
     {:component-did-mount #(let [node (r/dom-node %)
                                  page-offset (.getBoundingClientRect node)]
                              (swap! drag-state merge
                                     {:element-x (.-left page-offset) :element-y (.-top page-offset)}))
      :reagent-render
      (fn [c]
        [:div (merge
               {:style (merge
                        {:display "inline-block"
                         :width size :height size
                         :box-sizing "border-box"
                         :background-color (cond
                                             (and @hover blank?) "#eeeeee"
                                             blank? "#ffeaea"
                                             :otherwise "#dbfaae")
                         :margin-top -1 :margin-left -1
                         :border "1px solid black"
                         :font-weight "bold"
                         :font-size (str (-> size (* 3) (/ 4)) "px")
                         :line-height (str size "px")
                         :vertical-align "middle"
                         :text-align "center"
                         }
                        (when (:dragging @drag-state)
                          {:position "absolute"
                           :left (:left @drag-state)
                           :top (:top @drag-state)}))}
               (when blank?
                 {:on-mouse-over #(do (js/console.log "blah") (reset! hover true))
                  :on-mouse-out #(reset! hover false)})
               (when-not blank?
                 {:on-mouse-down #(when (zero? (.-button %))
                                    (.stopPropagation %)
                                    (let [move-handler (partial mouse-move drag-state)
                                          up-handler (partial mouse-up drag-state)
                                          new-state {:mouse-down true
                                                     :origin-x (.-pageX %)
                                                     :origin-y (.-pageY %)
                                                     :move-handler move-handler
                                                     :up-handler up-handler}]
                                      (swap! drag-state merge new-state)
                                      (js/document.addEventListener "mousemove" move-handler)
                                      (js/document.addEventListener "mouseup" up-handler)))}))
         (string/upper-case c)])})))

(defn diagram [{:keys [horiz vert] :as v}]
  (into
   [:div {:style {:display "inline-block"
                  :margin-left 50}}]
   (let [divs (for [j (range 5)
                    i (range 5)
                    :let [blank? (#{[0 0] [0 4] [4 0] [4 4]} [i j])]]
                (into
                 [(if blank?
                    [blank]
                    [letter
                     (str
                      (cond
                        (= i 0) (first (nth horiz (dec j)))
                        (= i 4) (last (nth horiz (dec j)))
                        (= j 0) (first (nth vert (dec i)))
                        (= j 4) (last (nth vert (dec i)))
                        :otherwise " "))])]
                 (when (and (= i 4) (< j 4))
                   [[:br]])))]
     (apply concat divs))))

(defn word [w]
  [:div {:style {:display "inline-block"}}
   (for [[i c] (map-indexed vector w)]
     ^{:key (str w "-" i)}
     [letter c])])

(defn game []
  (let [zag (subscribe [:zag])]
    [:div
     (for [z @zag]
       ^{:key (str "diag-" (:word z))}
       [diagram z])
     [:br]
     (for [z @zag]
       ^{:key (str "word-" (:word z))}
       [word (:word z)])]))

(defn main []
  [:div
   [mui/app-bar {:title "I Ty możesz zostać Kobremem!"}]
   [game]])

(defn page []
  [mui/theme-provider
   [main]])

(defn root []
  [page])

(defn init []
  (js/injectTapEventPlugin)
  (dispatch-sync [:init])
  (r/render [root] (.getElementById js/document "app")))

(js/document.addEventListener "DOMContentLoaded" init)
