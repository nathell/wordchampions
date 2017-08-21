(ns gridlock
  (:require [clojure.java.io :as io]
            [clojure.string :as string]
            polelum))

(def piatki (with-open [f (io/reader "w5.txt")] (vec (line-seq f))))
(def dziewiatki (with-open [f (io/reader "w9.txt")] (vec (line-seq f))))
(def sample (with-open [f (io/reader "sample.dat")] (vec (line-seq f))))

(defn podst? [w]
  (let [s (polelum/stem w)]
    (some #(= (:base %) w) s)))

(def ppiatki (filter podst? piatki))
(def pdziewiatki (filter podst? dziewiatki))

(defn wsort [w] (apply str (sort w)))

(def grp9 (group-by wsort pdziewiatki))
(def set9 (set (map wsort pdziewiatki)))

(def grp5 (group-by #(subs % 1 4) ppiatki))
(def kgrp5 (set (keys grp5)))

(defn invert-threes [^String a ^String b ^String c]
  [(str (.charAt a 0) (.charAt b 0) (.charAt c 0))
   (str (.charAt a 1) (.charAt b 1) (.charAt c 1))
   (str (.charAt a 2) (.charAt b 2) (.charAt c 2))])

(defn complete [a b c]
  {:middles [a b c],
   :words (grp9 (wsort (str a b c))),
   :horiz (mapv grp5 [a b c]),
   :vert (mapv grp5 (invert-threes a b c))})

(defn zadania-one [a]
  (println "Searching" a)
  (for [b kgrp5 c kgrp5
        :let [w1 (str (.charAt a 0) (.charAt b 0) (.charAt c 0))
              w2 (str (.charAt a 1) (.charAt b 1) (.charAt c 1))
              w3 (str (.charAt a 2) (.charAt b 2) (.charAt c 2))]
        :when (and (kgrp5 w1) (kgrp5 w2) (kgrp5 w3) (set9 (wsort (str a b c))))]
    (complete a b c)))

(defn zadania []
  (for [a (sort kgrp5)
        :let [_ (println "Searching" a)]
        b kgrp5 c kgrp5
        :let [[w1 w2 w3] (invert-threes a b c)]
        :when (and (kgrp5 w1) (kgrp5 w2) (kgrp5 w3) (set9 (wsort (str a b c))))]
    (complete a b c)))

#_(def zad (doall (zadania)))

(defn reconstruct-task [w]
  (let [w1 (subs w 0 3)
        w2 (subs w 3 6)
        w3 (subs w 6 9)
        v1 (str (nth w 0) (nth w 3) (nth w 6))
        v2 (str (nth w 1) (nth w 4) (nth w 7))
        v3 (str (nth w 2) (nth w 5) (nth w 8))]
    {:word (rand-nth (grp9 (wsort w)))
     :horiz (mapv grp5 [w1 w2 w3])
     :vert (mapv grp5 [v1 v2 v3])}))

(defn diagram [{:keys [horiz vert]}]
  (let [w1 (map rand-nth horiz)
        w2 (map rand-nth vert)]
    (with-out-str
      (println (apply str " " (map first w2)))
      (doseq [x w1] (println (str (first x) "..." (last x))))
      (println (apply str " " (map last w2))))))
