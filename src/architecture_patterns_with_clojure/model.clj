(ns architecture-patterns-with-clojure.model
  (:require [architecture-patterns-with-clojure.util.date :as date]
            [architecture-patterns-with-clojure.util.exception :refer [make-ex-info]]))


(defn out-of-stock [line]
  (make-ex-info {:message (str "Out of stock for sku " (:sku line))
                 :data    [line]}))

(defrecord OrderLine [order_id sku quantity])

(defn new-order-line [{:keys [order_id sku quantity]}]
  (->OrderLine order_id sku quantity))


(defrecord Batch [ref sku quantity eta  allocations])

(defn new-batch [{:keys [ref sku quantity eta] :or {eta (date/now)}}]
  (map->Batch {:ref                ref
               :sku                sku
               :quantity           quantity
               :eta                eta
               :allocations        #{}})
  )

(defn available-quantity [batch]
  (let [allocated (reduce #(+ (:quantity %2) %1) 0 (:allocations batch))]
    (- (:quantity batch) allocated))
  )

(defn can-allocate? [line batch]
  (and (= (:sku batch) (:sku line))
       (>= (available-quantity batch) (:quantity line))
       )
  )

(defn batches-available [line batches]
  (filter #(can-allocate? line %) batches)
  )

(defn allocate
  ([line batch & rest]
   (let [batches (batches-available line (conj rest batch))]
     (when (empty? batches) (throw (out-of-stock line)))
     (allocate line (apply min-key #(date/to-inst-ms (:eta %)) batches))
     ))
  ([line batch]
   (when (zero? (available-quantity batch))
     (throw (out-of-stock line)))
   (if (can-allocate? line batch)
     (update batch :allocations conj line)
     batch))
  )

(defn deallocate [line batch]
  (update batch :allocations disj line))

