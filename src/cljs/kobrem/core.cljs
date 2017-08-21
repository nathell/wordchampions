(ns kobrem.core
  (:require [reagent.core :as r]
            [re-frame.core :refer [reg-event-db reg-event-fx reg-sub dispatch dispatch-sync subscribe]]
            [kobrem.mui :as mui]
            [clojure.string :as string]
            [kobrem.problems :as problems]))

(enable-console-print!)

(defn substitute-letter [w i c]
  (str (subs w 0 i) c (subs w (inc i) (count w))))

(defn convert-problem [{:keys [word horiz vert]}]
  {:word word, :horiz (mapv rand-nth horiz), :vert (mapv rand-nth vert), :fill "........."})

(defn problems []
  (let [res (map convert-problem (take 5 (shuffle problems/problems)))
        words (map (fn [x] {:full (:word x), :available (:word x)})
                   res)]
    {:zag (mapv #(dissoc % :word) res)
     :words (vec (shuffle words))}))

(reg-event-db
 :init
 (fn [_ _]
   {:mode :before-start :time 0}))

(reg-event-fx
 :start
 (fn [{db :db} _]
   (let [prs (problems)]
     (merge (when-not (= (:mode db) :in-progress)
              {:dispatch [:tick]})
            {:db (merge db prs
                        {:mode :in-progress
                         :time 0
                         :compat {}})}))))

(reg-event-fx
 :tick
 (fn [{db :db} _]
   (if (= (:mode db) :in-progress)
     {:db (update db :time inc)
      :dispatch-later [{:ms 1000 :dispatch [:tick]}]}
     {})))

(defn invmap [m]
  (zipmap (vals m) (keys m)))

(reg-event-db
 :inc
 (fn [db _]
   (update-in db [:counter] inc)))

(reg-event-db
 :reset
 (fn [db _]
   (assoc db
          :words (mapv #(assoc % :available (:full %)) (:words db))
          :zag (mapv #(assoc % :fill ".........") (:zag db))
          :compat {})))

(defn return-letter [db]
  (let [{:keys [diagram pos]} (:dragged-letter db)]
    (if diagram
      (let [iword (get-in db [:compat diagram])
            word (get-in db [:words iword :full])
            aword (get-in db [:words iword :available])
            w (get-in db [:zag diagram :fill])
            c (nth w pos)
            i (first (filter #(and (= (nth word %) c) (= (nth aword %) "."))
                             (range 9)))
            new-fill (substitute-letter (get-in db [:zag diagram :fill]) pos ".")]
        (-> db
            (assoc-in [:zag diagram :fill] new-fill)
            (update-in [:words iword :available]
                       #(substitute-letter % i c))
            (update-in [:compat diagram]
                       (if (= new-fill ".........") (constantly nil) identity))
            (dissoc :dragged-letter)))
      (dissoc db :dragged-letter))))

(defn success?
  ([db]
   (reduce #(and %1 %2)
             (map (partial success? db)
                  (range (count (:zag db))))))
  ([db i]
   (let [{:keys [horiz fill]} (get-in db [:zag i])
         [a b c] horiz]
     (= (str (subs a 1 4) (subs b 1 4) (subs c 1 4)) fill))))

(reg-event-db
 :move
 (fn [db [_ target]]
   (let [new-db
         (if (= target :word-bag)
           (return-letter db)
           (let [{:keys [diagram pos]} target
                 {:keys [word letter]} (:dragged-letter db)]
             (if word
               (let [w (get-in db [:words word :full])
                     c (subs w letter (inc letter))]
                 (-> db
                     (update-in [:zag diagram :fill]
                                #(substitute-letter % pos c))
                     (update-in [:words word :available]
                                #(substitute-letter % letter "."))
                     (assoc-in [:compat diagram] word)
                     (dissoc :dragged-letter)))
               (let [{source-diagram :diagram, source-pos :pos} (:dragged-letter db)
                     w (get-in db [:zag source-diagram :fill])
                     c (subs w source-pos (inc source-pos))]
                 (-> db
                     (update-in [:zag diagram :fill] #(substitute-letter % pos c))
                     (update-in [:zag source-diagram :fill] #(substitute-letter % source-pos "."))
                     (dissoc :dragged-letter))))))]
     (if (success? new-db)
       (assoc new-db :mode :success)
       new-db))))

(reg-event-db
 :set-dragged-letter
 (fn [db [_ pos]]
   (assoc db :dragged-letter pos)))

(reg-sub
 :success?
 (fn [db [_ i]]
   (if i
     (success? db i)
     (reduce #(and %1 %2)
             (map (partial success? db)
                  (range (count (:zag db))))))))

(reg-sub
 :compat
 (fn [db _]
   (:compat db)))

(defn leading-zero
  [s]
  (if (< s 10)
    (str "0" s)
    (str s)))

(reg-sub
 :time
 (fn [db _]
   (let [t (:time db)]
     (str (leading-zero (int (/ t 60))) ":" (leading-zero (mod t 60))))))

(reg-sub
 :zag
 (fn [db _]
   (:zag db)))

(reg-sub
 :mode
 (fn [db _]
   (:mode db)))

(reg-sub
 :dragged-letter
 (fn [db _]
   (:dragged-letter db)))

(reg-sub
 :available-words
 (fn [db _]
   (mapv :available (:words db))))

(reg-sub
 :can-drop
 (fn [db [_ diag]]
   (and (:dragged-letter db)
        (if (:word (:dragged-letter db))
          (let [compat (:compat db)
                inv (invmap compat)
                v (inv (:word (:dragged-letter db)))]
            (or (and (not v) (not (compat diag)))
                (= v diag)))
          (= diag (:diagram (:dragged-letter db)))))))

(def size 45)

(def html-context (js/ReactDnD.DragDropContext js/ReactDnDHTML5Backend))
(def drag-source js/ReactDnD.DragSource)
(def drop-target js/ReactDnD.DropTarget)

(defn decorate [decorator]
  (fn [component]
    (r/adapt-react-class
     (decorator (r/reactify-component component)))))

(def with-dnd-context (decorate html-context))

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

(defn basic-letter [c hover?]
  (let [blank? (= (str c) ".")]
    [:div {:class (if blank? "blank" "letter")
           :style {:display "inline-block"
                   :width size :height size
                   :box-sizing "border-box"
                   :background-color (cond
                                       hover? "#ccc"
                                       blank? "#ffeaea"
                                       :otherwise "#eee")
                   :margin-top -1 :margin-left -1
                   :border "1px solid black"
                   :font-weight "bold"
                   :font-size (str (-> size (* 3) (/ 4)) "px")
                   :line-height (str size "px")
                   :vertical-align "middle"
                   :text-align "center"
                   }}
     (when-not blank?
       (string/upper-case c))]))

(defn letter [pos c]
  (let [blank? (= (str c) ".")]
    (if blank?
      (let [f (decorate (drop-target "letter"
                                     (clj->js {:canDrop (fn [props] @(subscribe [:can-drop (:diagram pos)]))
                                               :drop #(do
                                                        (dispatch-sync [:move pos])
                                                        js/undefined)})
                                     (fn [connect monitor]
                                       (clj->js {:connectDropTarget (.dropTarget connect)
                                                 :isOver (.isOver monitor)}))))]
        [(f (r/create-class
             {:reagent-render (fn []
                                (let [props (r/props (r/current-component))
                                      connect-drop-target (:connectDropTarget props)]
                                  (connect-drop-target (r/as-element (basic-letter c (:isOver props))))))}))])
      (let [f (decorate (drag-source "letter"
                                     (clj->js {:beginDrag #(do
                                                             (dispatch-sync [:set-dragged-letter pos])
                                                             (clj->js {}))})
                                     (fn [connect monitor]
                                       (clj->js {:connectDragSource (.dragSource connect)
                                                 :isDragging (.isDragging monitor)}))))]
        [(f (r/create-class
             {:reagent-render (fn []
                                (let [connect-drag-source (:connectDragSource (r/props (r/current-component)))]
                                  (connect-drag-source (r/as-element (basic-letter c false)))))}))]))))

(defn diagram [diag {:keys [horiz vert fill] :as v}]
  (into
   [:div {:style {:padding 10
                  :margin 10
                  :display "inline-block"
                  :border "1px solid #aaa"
                  :background (cond
                                @(subscribe [:success? diag]) "#bfa"
                                @(subscribe [:can-drop diag]) "#ccc"
                                :else "#fec")
                  :border-radius 10}}]
   (let [divs (for [j (range 5)
                    i (range 5)
                    :let [blank? (#{[0 0] [0 4] [4 0] [4 4]} [i j])
                          border? (or (= i 0) (= i 4) (= j 0) (= j 4))]]
                (into
                 [(cond
                    blank? [blank]
                    border? [basic-letter
                             (str
                              (cond
                                (= i 0) (first (nth horiz (dec j)))
                                (= i 4) (last (nth horiz (dec j)))
                                (= j 0) (first (nth vert (dec i)))
                                (= j 4) (last (nth vert (dec i)))))]
                    :otherwise (let [pos (+ (* (dec j) 3) (dec i))
                                     c (subs fill pos (inc pos))]
                                 [letter {:diagram diag, :pos pos} c]))]
                 (when (and (= i 4) (< j 4))
                   [[:br]])))]
     (apply concat divs))))

(defn word [n w]
  [:div {:style {:display "block"
                 :margin-right 30
                 :margin-top (if (zero? n) 0 30)}
         :class "word"}
   (for [[i c] (map-indexed vector w) :when (not= c \.)]
     ^{:key (str w "-" i)}
     [letter {:word n, :letter i} c])])

(defn basic-word-bag []
  [:div {:style {:padding 10
                 :margin 10
                 :display "inline-block"
                 :vertical-align "middle"
                 :width 450
                 :border "1px solid #aaa"
                 :background (if @(subscribe [:dragged-letter]) "#ccc" "#fec")
                 :border-radius 10}}
   (for [[i w] (map-indexed vector @(subscribe [:available-words]))]
     ^{:key (str "word-" i)}
     [word i w])])

(defn word-bag []
  (let [f (decorate (drop-target "letter"
                                 (clj->js {:drop #(do
                                                    (dispatch-sync [:move :word-bag])
                                                    js/undefined)})
                                 (fn [connect monitor]
                                   (clj->js {:connectDropTarget (.dropTarget connect)
                                             :isOver (.isOver monitor)}))))]
    [(f (r/create-class
         {:reagent-render (fn []
                            (let [props (r/props (r/current-component))
                                  connect-drop-target (:connectDropTarget props)]
                              (connect-drop-target (r/as-element (basic-word-bag)))))}))]))

(defn time-widget []
  [:div {:style {:padding 10
                 :margin 10
                 :display "inline-block"
                 :width 230
                 :border "1px solid #aaa"
                 :background "#fec"
                 :border-radius 10
                 :height 100
                 :font-size "60pt"
                 :line-height "60pt"
                 :font-weight "bold"
                 :vertical-align "middle"
                 }}
   @(subscribe [:time])])

(defn game []
  [mui/paper
   (for [[i z] (map-indexed vector @(subscribe [:zag]))]
     ^{:key (str "diag-" i)}
     [diagram i z])
   [:br]
   [word-bag]
   [time-widget]
   [:br]
   [mui/raised-button {:label "Resetuj"
                       :primary true
                       :on-touch-tap #(dispatch [:reset])}]
   [mui/raised-button {:label "Nowa gra"
                       :secondary true
                       :on-touch-tap #(dispatch [:start])}]])

(defn welcome []
  [mui/card
   [mui/card-header "Witaj!"]
   [mui/card-actions
    [mui/raised-button {:label "Zacznij grę"
                        :primary true
                        :on-touch-tap #(dispatch [:start])}]]])

(defn success []
  [mui/card
   [mui/card-header (str "Brawo! Czas: " @(subscribe [:time]))]
   [mui/card-actions
    [mui/raised-button {:label "Jeszcze raz"
                        :primary true
                        :on-touch-tap #(dispatch [:start])}]]])

(defn main []
  [:div
   [mui/app-bar {:title "I Ty możesz zostać Kobremem!"}]
   (condp = @(subscribe [:mode])
     :before-start [welcome]
     :success [success]
     [game])])

(defn page []
  [mui/theme-provider
   [main]])

(defn root []
  [(with-dnd-context page)])

(defn init []
  (js/injectTapEventPlugin)
  (dispatch-sync [:init])
  (r/render [root] (.getElementById js/document "app")))

(js/document.addEventListener "DOMContentLoaded" init)
