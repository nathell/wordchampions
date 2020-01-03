(ns gridlock.subs
  (:require
    [gridlock.db :as db]
    [gridlock.i18n :as i18n]
    [re-frame.core :refer [reg-sub]]))

(defn finished?
  [{:keys [horiz vert fill]}]
  (let [[a b c] horiz]
    (= (str (subs a 1 4) (subs b 1 4) (subs c 1 4)) fill)))

(defn- msg
  [db id]
  (get-in i18n/i18n [(:language db) id]))

(defn can-place?
  [{:keys [compat current-tile dragging]} diagram-number]
  (let [source (or dragging current-tile)
        acceptable-source (get compat diagram-number)
        diagram-source (:diagram-number source)
        nine-source (:nine-number source)]
    (cond diagram-source (= diagram-source diagram-number)
          nine-source (or (= acceptable-source nine-source)
                          (and (nil? acceptable-source)
                               (not (contains? (set (vals compat)) nine-source))))
          :otherwise false)))

(defn ->diagram
  [db i {:keys [horiz vert fill] :as zag}]
  {:diagram-number i
   :top (apply str (map first vert))
   :bottom (apply str (map last vert))
   :left (apply str (map first horiz))
   :right (apply str (map last horiz))
   :placed fill
   :highlighted (can-place? db i)
   :finished (finished? zag)})

(reg-sub
  :diagrams
  (fn [db _]
    (map-indexed (partial ->diagram db) (:zag db))))

(reg-sub
  :nines
  (fn [{:keys [words]} _]
    (map-indexed (fn [i {:keys [available]}]
                   {:nine-number i, :letters available})
                 words)))

(reg-sub
  :can-place?
  (fn [db [_ diagram-number]]
    (can-place? db diagram-number)))

(reg-sub
  :mode
  (fn [db _]
    (:mode db)))

(reg-sub
  :current-diagram
  (fn [db _]
    (:current-diagram db)))

(defn leading-zero
  [s]
  (if (< s 10)
    (str "0" s)
    (str s)))

(defn format-time [t]
  (str (leading-zero (int (/ t 60))) ":" (leading-zero (mod t 60))))

(reg-sub
  :title-bar
  (fn [db _]
    (if (contains? #{:in-progress :success} (:mode db))
      (str (msg db :time)
           ": "
           (format-time (:time db))
           " "
           (msg db :hints)
           ": "
           (:hints db))
      (msg db :word-champions))))

(reg-sub
  :dictionary-selected?
  (fn [db [_ dict]]
    (= (:dictionary db) dict)))

(reg-sub
  :loaded?
  (fn [{:keys [dictionaries]} _]
    (and (:nkjp dictionaries) (:osps dictionaries) true)))

(reg-sub
  :current-tile
  (fn [db _]
    (:current-tile db)))

(reg-sub
  :language
  (fn [db _]
    (:language db)))

(reg-sub
  :i18n-message
  :<- [:language]
  (fn [language [_ id]]
    (get-in i18n/i18n [language id])))
