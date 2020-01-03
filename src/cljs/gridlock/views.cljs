(ns gridlock.views
  (:require
    [clojure.string :as string]
    [re-frame.core :as rf :refer [dispatch]]
    [reagent.core :as r]))

(def <sub (comp deref rf/subscribe))

(defn msg [id]
  (<sub [:i18n-message id]))

(defn merge-props
  ([m] m)
  ([m1 m2] (r/merge-props m1 m2))
  ([m1 m2 & ms] (r/merge-props m1 (apply merge-props m2 ms))))

(defn diagram-corner []
  [:div.diagram-corner])

(defn tile [source c movable?]
  (if movable?
    (r/with-let [hide? (r/atom nil)]
      [:div.tile
       (merge-props
        {:draggable true
         :on-drag-start #(do (-> % (aget "dataTransfer") (.setData "text/plain" c))
                             (dispatch [:start-drag source])
                             (reset! hide? true))
         :on-drag-end #(do (dispatch [:end-drag])
                           (reset! hide? false))
         :on-touch-start #(dispatch [:tap-tile source])
         :class (when @hide? "tile-hidden")}
        (when (= source (<sub [:current-tile]))
          {:class "tile-current"}))
       c])
    [:div.tile c]))

(defn- border-placing [x y]
  (cond (= x 0) [:left   (dec y)]
        (= x 4) [:right  (dec y)]
        (= y 0) [:top    (dec x)]
        (= y 4) [:bottom (dec x)]))

(defn diagram
  [{:keys [diagram-number placed finished highlighted] :as desc}]
  [:div.diagram (merge-props (when finished {:class "diagram-finished"})
                             (when highlighted {:class "diagram-highlighted"})
                             (if (= diagram-number (<sub [:current-diagram]))
                               {:class "diagram-current"}
                               {:class "diagram-small"})
                             {:on-click #(dispatch [:set-current-diagram diagram-number])})
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
        (if (and (= x 4) (= y 0) (not finished))
          ^{:key (str x "-" y)}
          [:div.reload
           [:svg {:on-click #(dispatch [:clean-diagram diagram-number])}
            [:use {:xlink-href "#rotate-ccw"}]]]
          ^{:key (str x "-" y)}
          [:div.diagram-item
           (cond->
               {:class (cond corner? "diagram-item-corner"
                             border  "diagram-item-border")}
             (and in-area? (@highlighted-fields letter-pos)) (r/merge-props {:class "diagram-item-highlighted"})
             droppable? (assoc :on-drag-enter #(swap! highlighted-fields conj letter-pos)
                               :on-drag-leave #(swap! highlighted-fields disj letter-pos)
                               :on-drag-over #(do (.preventDefault %) (.stopPropagation %))
                               :on-drop #(do (.preventDefault %) (swap! highlighted-fields empty) (dispatch [:drop-diagram {:diagram-number diagram-number, :letter-pos letter-pos}]))
                               :on-touch-start #(dispatch [:tap-diagram {:diagram-number diagram-number, :letter-pos letter-pos}])))
           (cond corner? nil
                 border  (get-in desc [border border-pos])
                 :else   (when (and letter (not placable?))
                           [tile {:diagram-number diagram-number, :letter-pos letter-pos} letter (not finished)]))]))))])

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
    [:button.button {:on-click #(dispatch [:hint])} (msg :hint)]
    [:button.button {:on-click #(dispatch [:reset])} (msg :reset)]
    [:button.button {:on-click #(dispatch [:restart])} (msg :new-game)]]])

(defn language-picker []
  (let [current-language (<sub [:language])
        languages [:pl :en]]
    (into
     [:div.language-picker]
     (for [language languages]
       [:img {:src (str "img/" (name language) ".svg")
              :class (when (= language current-language) "selected")
              :on-click #(dispatch [:set-language language])}]))))

(defn welcome []
  [:div.welcome
   [language-picker]
   [:div.title (msg :hello)]
   [:div.buttons
    [:button.button.start-button
     {:on-click #(dispatch [:select-difficulty])}
     (msg :start-game)]
    [:a.button {:target "_blank", :rel "noopener", :href "https://www.youtube.com/watch?v=7ec6j31nlAk"}
     (msg :how-to-play)]]])

(defn success []
  [:div.panel.success
   [diagrams-area]
   [:h1 (msg :hooray)]
   [:div.buttons
    [:button.button
     {:on-click #(dispatch [:restart])}
     (msg :play-again)]]])

(defn difficulty []
  [:div.panel.difficulty
   (when (= (<sub [:language]) :pl)
     [:div
      [:h2 "Wybierz słownik:"]
      [:div.dictionaries
       [:div.dictionary
        {:class (when (<sub [:dictionary-selected? :nkjp]) "selected")
         :on-click #(dispatch [:set-dictionary :nkjp])}
        [:h3 "Częste słowa"]
        [:p "(w tym imiona i inne nazwy własne)"]
        [:p "279 zagadek"]]
       [:div.dictionary
        {:class (when (<sub [:dictionary-selected? :osps]) "selected")
         :on-click #(dispatch [:set-dictionary :osps])}
        [:h3 "Oficjalny Słownik Polskiego Scrabblisty"]
        [:p "(trudne słowa, bez nazw własnych)"]
        [:p "2 000 zagadek"]
        [:p [:small "Udostępniony dzięki uprzejmości "
             [:a {:href "http://pfs.org.pl"
                  :target "_blank"
                  :rel "noopener"} "Polskiej Federacji Scrabble"]]]]]])
   [:h2 (msg :select-difficulty)]
   [:div.buttons
    [:button.button {:on-click #(dispatch [:start 1])} (msg :easy)]
    [:button.button {:on-click #(dispatch [:start 3])} (msg :normal)]
    [:button.button {:on-click #(dispatch [:start 5])} (msg :hard)]]])

(defn root []
  [:div.root
   [:div.title-bar (<sub [:title-bar])]
   (if-not (<sub [:loaded?])
     [:div.main-panel (msg :loading-dictionaries) "..."]
     [:div.main-panel
      (condp = (<sub [:mode])
        :before-start [welcome]
        :difficulty [difficulty]
        :success [success]
        [game])])
   [:div.author "© "
    [:a {:target "_blank", :rel "noopener", :href "http://danieljanus.pl"} "Daniel Janus"]
    " 2017–2019"
    [:span.is-hidden-mobile
     " | "
     (msg :written-in-cljs)
     " | "
     [:a {:target "_blank", :rel "noopener", :href "https://github.com/nathell/gridlock"} (msg :source-code)]]]])
