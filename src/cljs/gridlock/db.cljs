(ns gridlock.db
  (:require
    [gridlock.problems :as problems]))

(defn substitute-letter [w i c]
  (str (subs w 0 i) c (subs w (inc i) (count w))))

(defn convert-problem [{:keys [word horiz vert]}]
  {:word word,
   :horiz (mapv rand-nth horiz),
   :vert (mapv rand-nth vert),
   :fill "........."})

(defn problems
  [n]
  (let [res (map convert-problem (take n (shuffle problems/problems)))
        words (map-indexed (fn [i x] {:diagram-number i, :full (:word x), :available (:word x)})
                           res)]
    {:zag (mapv #(dissoc % :word) res)
     :words (vec (shuffle words))}))

(defn expected-letters
  [{[a b c] :horiz}]
  (str (subs a 1 4) (subs b 1 4) (subs c 1 4)))

(defn success?
  ([db]
   (reduce #(and %1 %2)
             (map (partial success? db)
                  (range (count (:zag db))))))
  ([db i]
   (let [problem (get-in db [:zag i])]
     (= (expected-letters problem) (:fill problem)))))

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
