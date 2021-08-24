(ns architecture-patterns-with-clojure.domain.batch
  (:require [architecture-patterns-with-clojure.util.date :as date]))

(defrecord Batch [ref sku quantity eta allocations])

(defn new-batch [{:keys [ref sku quantity eta] :or {eta (date/now)}}]
  (map->Batch {:ref         ref
               :sku         sku
               :quantity    quantity
               :eta         eta
               :allocations #{}}))

(defn allocated-quantity [batch]
  (reduce #(+ (:quantity %2) %1) 0 (:allocations batch)))

(defn available-quantity [batch]
  (- (:quantity batch) (allocated-quantity batch))
  )

(defn can-allocate? [batch line]
  (and (= (:sku batch) (:sku line))
       (>= (available-quantity batch) (:quantity line))))

(defn allocate [batch line]
  (update batch :allocations conj line))


(defn deallocate [batch & lines]
  (if (seq lines)
    (recur (update batch :allocations disj (first lines)) (rest lines))
    batch))

