(ns gridlock.db
  (:require [clojure.string :as string]
            [gridlock.cartesian :as cartesian]
            [gridlock.i18n :as i18n]))

(defn msg
  [db id]
  (get-in i18n/i18n [(:language db) id]))

(defn substitute-letter [w i c]
  (str (subs w 0 i) c (subs w (inc i) (count w))))

(defn convert-problem [{:keys [word horiz vert]}]
  {:word word,
   :horiz (mapv rand-nth horiz),
   :vert (mapv rand-nth vert),
   :fill "........."})

(defn rand-nth-with-index
  "Returns a 2-element vector [random index into given coll,
  element under that index]."
  [coll]
  (let [i (rand-int (count coll))]
    [i (nth coll i)]))

(defn make-problem
  [subparts variant]
  (let [[middles word h1 h2 h3 v1 v2 v3] (mapv nth subparts variant)
        middles (partition 3 middles)
        horiz-middles (map #(apply str %) middles)
        vert-middles (apply map str middles)
        horiz (mapv #(str (first %2) %1 (last %2)) horiz-middles [h1 h2 h3])
        vert (mapv #(str (first %2) %1 (last %2)) vert-middles [v1 v2 v3])]
    {:word word
     :horiz horiz
     :vert vert
     :fill "........."}))

(defn split-problem
  [problem]
  (let [parts (string/split problem #",")
        subparts (mapv #(string/split % #";") parts)
        dimensions (mapv count subparts)]
    {:subparts subparts, :dimensions dimensions}))

(defn parse-problem
  [n-problems [index problem]]
  (when problem
    (let [{:keys [subparts dimensions]} (split-problem problem)
          variant (mapv rand-int dimensions)
          all-dimensions (into [n-problems] dimensions)
          all-variant (into [index] variant)]
      (-> (make-problem subparts variant)
          (assoc :problem (cartesian/encode all-dimensions all-variant))))))

(defn retrieve-problem
  [db dict problem-number]
  (let [problems (get-in db [:dictionaries dict])
        n-problems (count problems)
        n (mod problem-number n-problems)
        problem (nth problems n)
        {:keys [subparts dimensions]} (split-problem problem)
        all-dimensions (into [n-problems] dimensions)
        variant (next (cartesian/decode all-dimensions problem-number))]
    (-> (make-problem subparts variant)
        (assoc :problem problem-number))))

(defn generate-problem
  [db dict]
  (let [problems (get-in db [:dictionaries dict])
        n-problems (count problems)]
    (->> problems
         rand-nth-with-index
         (parse-problem n-problems))))

(def dictionaries
  {:pl #{:osps :nkjp}, :en #{:en}})

(def dict->lang
  (into {}
        (for [[k vs] dictionaries v vs]
          [v k])))

(defn problems
  [db dict n]
  (let [items (if-let [problem-numbers (:problem-numbers db)]
                (mapv (partial retrieve-problem db dict) problem-numbers)
                (repeatedly n #(generate-problem db dict)))
        words (map-indexed (fn [i x] {:diagram-number i, :full (:word x), :available (:word x)})
                           items)]
    {:zag (mapv #(dissoc % :word) items)
     :words (vec (shuffle words))
     :url (string/join "/"
                       (into [(name dict)] (map :problem items)))}))

(defn parse-url [url]
  (let [parts (string/split url #"/")]
    (when parts
      (let [[dict & problems] parts
            dict (keyword dict)
            language (dict->lang dict)
            problems (mapv #(js/parseInt %) problems)]
        (when (and language
                   (every? int? problems)
                   (contains? #{1 3 5} (count problems)))
          {:language language,
           :dictionary dict,
           :mode :before-start-selected,
           :time 0,
           :problem-numbers problems})))))

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

(defn leading-zero
  [s]
  (if (< s 10)
    (str "0" s)
    (str s)))

(defn format-time [t]
  (str (leading-zero (int (/ t 60))) ":" (leading-zero (mod t 60))))

(defn share-message [{lang :language, time :time, hints :hints, :as db} url]
  (str (msg db :share-msg1)
       (msg db (case (-> db :zag count)
                 1 :share-easy
                 3 :share-medium
                 :share-hard))
       (msg db :share-msg2)
       (format-time time)
       (cond
         (and (= lang :pl) (zero? hints)) ", nie wykorzystując podpowiedzi!"
         (and (= lang :pl) (= hints 1)) ", wykorzystując 1 podpowiedź."
         (= lang :pl) (str ", wykorzystując " hints " podpowiedzi.")
         (and (= lang :en) (zero? hints)) ", not using any hints!"
         (and (= lang :en) (= hints 1)) ", using 1 hint."
         (= lang :en) (str ", using " hints " hints."))
       (msg db :share-msg3)
       (string/join ", " (map #(string/upper-case (:full %)) (:words db)))
       "\n\n"
       url))
