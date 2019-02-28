(ns gridlock.subs
  (:require
    [gridlock.db :as db]
    [re-frame.core :refer [reg-sub]]))

(defn finished?
  [{:keys [horiz vert fill]}]
  (let [[a b c] horiz]
    (= (str (subs a 1 4) (subs b 1 4) (subs c 1 4)) fill)))

(defn can-place?
  [{:keys [compat dragging]} diagram-number]
  (let [acceptable-source (get compat diagram-number)
        diagram-source (:diagram-number dragging)
        nine-source (:nine-number dragging)]
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
      (str "Czas: " (format-time (:time db)) " Podpowiedzi: " (:hints db))
      "Władcy słów")))
