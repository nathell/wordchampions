(ns gridlock.db
  (:require
    [clojure.string :as string]))

(defn substitute-letter [w i c]
  (str (subs w 0 i) c (subs w (inc i) (count w))))

(defn convert-problem [{:keys [word horiz vert]}]
  {:word word,
   :horiz (mapv rand-nth horiz),
   :vert (mapv rand-nth vert),
   :fill "........."})

(defn parse-problem
  [problem]
  (when problem
    (let [parts (string/split problem #",")
          [middles word h1 h2 h3 v1 v2 v3] (map #(rand-nth (string/split % #";")) parts)
          middles (partition 3 middles)
          horiz-middles (map #(apply str %) middles)
          vert-middles (apply map str middles)
          horiz (mapv #(str (first %2) %1 (last %2)) horiz-middles [h1 h2 h3])
          vert (mapv #(str (first %2) %1 (last %2)) vert-middles [v1 v2 v3])]
      {:word word
       :horiz horiz
       :vert vert
       :fill "........."})))

(defn generate-problem
  [db dict]
  (-> db
      (get-in [:dictionaries dict])
      rand-nth
      parse-problem))

(defn problems
  [db dict n]
  (let [items (repeatedly n #(generate-problem db dict))
        words (map-indexed (fn [i x] {:diagram-number i, :full (:word x), :available (:word x)})
                           items)]
    {:zag (mapv #(dissoc % :word) items)
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
