(ns gridlock.events
  (:require
    [ajax.core :as ajax]
    [clojure.string :as string]
    [day8.re-frame.http-fx]
    [gridlock.db :as db]
    [re-frame.core :refer [reg-event-db reg-event-fx]]))

(reg-event-db
  :start-drag
  (fn [db [_ dragged-letter]]
    (assoc db :dragging dragged-letter)))

(reg-event-db
  :end-drag
  (fn [db _]
    (assoc db :dragging nil)))

(reg-event-fx
  :fetch-dictionary
  (fn [_ [_ dict]]
    {:http-xhrio {:method :get
                  :uri (str "puzzles/" (name dict) ".txt")
                  :timeout 10000
                  :response-format (ajax/text-response-format)
                  :on-success [:fetch-dictionary-success dict]}}))

(reg-event-db
  :fetch-dictionary-success
  (fn [db [_ dict-name dict]]
    (assoc-in db [:dictionaries dict-name] (string/split dict #"\n"))))

(defn letter-at
  [db {:keys [diagram-number nine-number letter-pos]}]
  (cond
    diagram-number (get-in db [:zag diagram-number :fill letter-pos])
    nine-number (get-in db [:words nine-number :available letter-pos])))

(defn move-ok?
  [db source target]
  true)

(defn remove-from
  [db {:keys [diagram-number nine-number letter-pos] :as args}]
  (cond
    nine-number (update-in db [:words nine-number :available] db/substitute-letter letter-pos ".")
    diagram-number (update-in db [:zag diagram-number :fill] db/substitute-letter letter-pos ".")
    :otherwise db))

(defn insert-to-diagram
  [db {:keys [diagram-number letter-pos] :as args} letter]
  (update-in db [:zag diagram-number :fill] db/substitute-letter letter-pos letter))

(defn find-place-for-letter
  [{:keys [full available]} letter]
  (first
   (for [letter-number (range (count full))
         :when (and (= (.charAt full letter-number) letter)
                    (= (.charAt available letter-number) "."))]
     letter-number)))

(defn insert-to-nines
  [db nine-number letter]
  (if-let [letter-pos (find-place-for-letter (get-in db [:words nine-number]) letter)]
    (update-in db [:words nine-number :available] db/substitute-letter letter-pos letter)
    db))

(defn update-compat
  [db {source-nine :nine-number} {target-diagram :diagram-number}]
  (if source-nine
    (update db :compat assoc target-diagram source-nine)
    db))

(defn remove-compat-if-needed
  [db source-diagram target-nine]
  (let [{:keys [full available]} (get-in db [:words target-nine])]
    (if (= full available)
      (update db :compat dissoc source-diagram)
      db)))

(defn check-success [db]
  (if (db/success? db)
    (assoc db :mode :success)
    db))

(defn move-to-diagram [db source target]
  (let [letter (letter-at db source)]
    (if (move-ok? db source target)
      (-> db
          (remove-from source)
          (insert-to-diagram target letter)
          (update-compat source target)
          (check-success))
      db)))

(reg-event-db
  :drop-diagram
  (fn [db [_ target]]
    (let [source (:dragging db)]
      (move-to-diagram db source target))))

(defn move-to-nines [db source]
  (let [source-diagram (:diagram-number source)
        target-nine (get-in db [:compat source-diagram])
        letter (letter-at db source)]
    (if-not source-diagram
      db
      (-> db
          (remove-from source)
          (insert-to-nines target-nine letter)
          (remove-compat-if-needed source-diagram target-nine)))))

(defn clean-diagram [db diagram-number]
  (if-let [nine-number (get-in db [:compat diagram-number])]
    (-> db
        (update-in [:words nine-number] #(assoc % :available (:full %)))
        (assoc-in [:zag diagram-number :fill] ".........")
        (update :compat dissoc diagram-number))
    db))

(reg-event-db
  :clean-diagram
  (fn [db [_ diagram-number]]
    (clean-diagram db diagram-number)))

(reg-event-db
  :drop-nines
  (fn [db _]
    (move-to-nines db (:dragging db))))

(defn find-target
  [problem letter]
  (let [fill (:fill problem)
        expected-letters (db/expected-letters problem)]
    (first
     (for [[i candidate] (map-indexed vector expected-letters)
           :when (and (= candidate letter) (= (nth fill i) "."))]
       i))))

(defn all-letters-for-hint [db]
  (for [[nine-number nine] (map-indexed vector (:words db))
        [letter-pos letter] (map-indexed vector (:available nine))
        :when (not= letter ".")]
    [{:nine-number nine-number, :letter-pos letter-pos}
     {:diagram-number (:diagram-number nine), :letter-pos (find-target (get-in db [:zag (:diagram-number nine)]) letter)}]))

(defn all-wrong-letters [db]
  (for [[diagram-number problem] (map-indexed vector (:zag db))
        :let [expected (db/expected-letters problem)
              fill (:fill problem)]
        letter-pos (range 9)
        :when (and (not= (nth fill letter-pos) ".")
                   (or (not= (nth fill letter-pos) (nth expected letter-pos))
                       (when-let [word-number (get-in db [:compat diagram-number])]
                         (not= diagram-number (get-in db [:words word-number :diagram-number])))))]
    {:diagram-number diagram-number, :letter-pos letter-pos}))

(defn apply-hint
  [db]
  (if-let [wrong (seq (all-wrong-letters db))]
    (move-to-nines db (rand-nth wrong))
    (let [[source target] (rand-nth (all-letters-for-hint db))]
      (move-to-diagram db source target))))

(reg-event-db
  :hint
  (fn [db _]
    (-> db
        apply-hint
        (update :hints inc))))

(reg-event-fx
  :init
  (fn [_ _]
    {:db {:mode :before-start
          :time 0
          :dictionary :nkjp}
     :dispatch-n [[:fetch-dictionary :osps]
                  [:fetch-dictionary :nkjp]]}))

(reg-event-db
  :select-difficulty
  (fn [db _]
    (assoc db :mode :difficulty)))

(reg-event-fx
  :start
  (fn [{db :db} [_ n]]
    (let [prs (db/problems db (:dictionary db) n)]
      (merge (when-not (= (:mode db) :in-progress)
               {:dispatch [:tick]})
             {:db (merge db prs
                         {:mode :in-progress
                          :current-diagram 0
                          :time 0
                          :hints 0
                          :compat {}})}))))

(reg-event-fx
  :tick
  (fn [{db :db} _]
    (if (= (:mode db) :in-progress)
      {:db (update db :time inc)
       :dispatch-later [{:ms 1000 :dispatch [:tick]}]}
      {})))

(reg-event-db
  :reset
  (fn [db _]
    (assoc db
           :words (mapv #(assoc % :available (:full %)) (:words db))
           :zag (mapv #(assoc % :fill ".........") (:zag db))
           :compat {})))

(reg-event-db
  :restart
  (fn [db _]
    (assoc db :mode :before-start)))

(reg-event-db
  :set-dictionary
  (fn [db [_ dict]]
    (assoc db :dictionary dict)))

(reg-event-db
  :set-current-diagram
  (fn [db [_ diagram]]
    (assoc db :current-diagram diagram)))

(reg-event-db
  :tap-tile
  (fn [db [_ tile-source]]
    (if (:diagram-number tile-source)
      (move-to-nines db tile-source)
      (assoc db :current-tile tile-source))))

(reg-event-db
  :tap-diagram
  (fn [db [_ target]]
    (let [source (:current-tile db)]
      (if (and source (= (:current-diagram db) (:diagram-number target)))
        (-> db
            (move-to-diagram source target)
            (assoc :current-tile nil))
        db))))
