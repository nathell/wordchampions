(ns gridlock.cartesian)

(defn encode
  [[dim & other-dims] [x & xs]]
  (when (or (nil? dim) (nil? x))
    (throw (js/Error. (str "Dimension mismatch: " dim "," x))))
  (when (>= x dim)
    (throw (js/Error. (str "Entry out of dimension: " x "," dim))))
  (if (and (empty? other-dims) (empty? xs))
    x
    (+ (* dim (encode other-dims xs)) x)))

(defn decode
  [dims x]
  (loop [dims dims x x acc []]
    (if (empty? dims)
      acc
      (let [[dim & other-dims] dims
            n (mod x dim)
            x (/ (- x n) dim)]
        (recur other-dims x (conj acc n))))))
