(ns gridlock.views
  (:require
    [clojure.string :as string]
    [re-frame.core :as rf :refer [dispatch]]
    [reagent.core :as r]))

(def <sub (comp deref rf/subscribe))

(defn diagram-corner []
  [:div.diagram-corner])

(defn tile [source c movable?]
  (if movable?
    (r/with-let [hide? (r/atom nil)]
      [:div.tile {:draggable true
                  :on-drag-start #(do (dispatch [:start-drag source])
                                      (reset! hide? true))
                  :on-drag-end #(do (dispatch [:end-drag])
                                    (reset! hide? false))
                  :class (when @hide? "tile-hidden")}
       c])
    [:div.tile c]))

(defn- border-placing [x y]
  (cond (= x 0) [:left   (dec y)]
        (= x 4) [:right  (dec y)]
        (= y 0) [:top    (dec x)]
        (= y 4) [:bottom (dec x)]))

(defn diagram
  [{:keys [diagram-number placed finished highlighted] :as desc}]
  [:div.diagram (r/merge-props (when finished {:class "diagram-finished"})
                               (when highlighted {:class "diagram-highlighted"}))
   (r/with-let [highlighted-fields (r/atom #{})]
     (doall
      (for [y (range 5) x (range 5)
            :let [corner? (#{[0 0] [0 4] [4 0] [4 4]} [x y])
                  [border border-pos] (border-placing x y)
                  letter-pos (+ y y y x -4)
                  in-area? (not (or corner? border))
                  letter (when in-area? (nth placed letter-pos))
                  placable? (= letter ".")
                  droppable? (and placable? (<sub [:can-place? diagram-number]))]]
        ^{:key (str x "-" y)}
        [:div.diagram-item
         (cond->
             {:class (cond corner? "diagram-item-corner"
                           border  "diagram-item-border")}
           (and in-area? (@highlighted-fields letter-pos)) (r/merge-props {:class "diagram-item-highlighted"})
           droppable? (assoc :on-drag-enter #(swap! highlighted-fields conj letter-pos)
                             :on-drag-leave #(swap! highlighted-fields disj letter-pos)
                             :on-drag-over #(do (.preventDefault %) (.stopPropagation %))
                             :on-drop #(do (.preventDefault %) (swap! highlighted-fields empty) (dispatch [:drop-diagram {:diagram-number diagram-number, :letter-pos letter-pos}]))))
         (cond corner? nil
               border  (get-in desc [border border-pos])
               :else   (when (and letter (not placable?))
                         [tile {:diagram-number diagram-number, :letter-pos letter-pos} letter (not finished)]))])))])

(defn nine [{:keys [nine-number letters]}]
  (into
   [:div.nine]
   (for [[i letter] (map-indexed vector letters) :when (not= letter \.)]
     [tile {:nine-number nine-number, :letter-pos i} letter true])))

(defn nines-area []
  (r/with-let [highlighted (r/atom 0)] ; integer, not boolean,
    [:div.nines-area
     {:on-drag-enter #(swap! highlighted inc)
      :on-drag-leave #(swap! highlighted dec)
      :on-drag-over #(do (.preventDefault %) (.stopPropagation %))
      :on-drop #(do (.preventDefault %) (reset! highlighted 0) (dispatch [:drop-nines]))
      :class (when (pos? @highlighted) "nines-area-highlighted")}
     (for [word (<sub [:nines])]
       ^{:key (:nine-number word)}
       [nine word])]))

(defn diagrams-area []
  [:div.diagrams-area
   (for [item (<sub [:diagrams])]
     ^{:key (:diagram-number item)}
     [diagram item])])

(defn game []
  [:div.panel.game
   [diagrams-area]
   [nines-area]
   [:div.buttons
    [:button.button {:on-click #(dispatch [:hint])} "Podpowiedź"]
    [:button.button {:on-click #(dispatch [:reset])} "Resetuj"]
    [:button.button {:on-click #(dispatch [:restart])} "Nowa gra"]]])

(defn welcome []
  [:div.welcome
   [:div.title "Witaj!"]
   #_
   [:iframe {:width 560, :height 315, :src "https://www.youtube.com/embed/7ec6j31nlAk", :frame-border "0", :allow-full-screen true}]
   [:div.buttons
    [:button.button.start-button
     {:on-click #(dispatch [:select-difficulty])}
     "Zacznij grę"]
    [:a.button {:target "_blank", :rel "noopener", :href "https://www.youtube.com/watch?v=7ec6j31nlAk"}
     "Jak grać?"]]])

(defn success []
  [:div.panel.success
   [diagrams-area]
   [:h1 "Brawo!"]
   [:div.buttons
    [:button.button
     {:on-click #(dispatch [:restart])}
     "Jeszcze raz"]]])

(defn difficulty []
  [:div.panel.difficulty
   [:h2 "Wybierz poziom trudności"]
   [:div.buttons
    [:button.button {:on-click #(dispatch [:start 1])} "Łatwy"]
    [:button.button {:on-click #(dispatch [:start 3])} "Średni"]
    [:button.button {:on-click #(dispatch [:start 5])} "Trudny"]]])

(defn root []
  [:div.root
   [:div.title-bar (<sub [:title-bar])]
   [:div.main-panel
    (condp = (<sub [:mode])
      :before-start [welcome]
      :difficulty [difficulty]
      :success [success]
      [game])]
   [:div.author "© "
    [:a {:target "_blank", :rel "noopener", :href "http://danieljanus.pl"} "Daniel Janus"]
    " 2017–2019 | Napisane w języku ClojureScript | "
    [:a {:target "_blank", :rel "noopener", :href "https://github.com/nathell/gridlock"} "Kod źródłowy"]
    ]
   ])
      ;; :before-start [welcome]
      ;; :success [success]
      ;; [game])])]])
