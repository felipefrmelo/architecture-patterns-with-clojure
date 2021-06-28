(ns architecture-patterns-with-clojure.model
  (:require [architecture-patterns-with-clojure.util.date :as date]
            [architecture-patterns-with-clojure.util.exception :refer [make-ex-info]]))


(defn out-of-stock [line]
  (make-ex-info {:message (str "Out of stock for sku " (:sku line))
                 :data    [line]}))

(defrecord OrderLine [order-id sku quantity])

(defn new-order-line [{:keys [order-id sku quantity]}]
  (->OrderLine order-id sku quantity))


(defrecord Batch [ref sku quantity eta available-quantity allocations])

(defn new-batch [{:keys [ref sku quantity eta] :or {eta (date/now)}}]
  (map->Batch {:ref                ref
               :sku                sku
               :quantity           quantity
               :available-quantity quantity
               :eta                eta
               :allocations        #{}})
  )

(defn can-allocate? [line batch]
  (and (= (:sku batch) (:sku line))
       (>= (:available-quantity batch) (:quantity line))
       (not (contains? (:allocations batch) line))
       )
  )

(defn batches-available [line batches]
  (filter #(can-allocate? line %) batches)
  )

(defn allocate
  ([line batch & rest]
   (let [batches (batches-available line (conj rest batch))]
     (when (empty? batches) (throw (out-of-stock line)))
     (allocate line (apply min-key #(inst-ms (:eta %)) batches))))
  ([line batch]
   (when (zero? (:available-quantity batch))
     (throw (out-of-stock line)))
   (if (can-allocate? line batch)
     (-> batch (update :allocations conj line)
         (update :available-quantity - (:quantity line)))
     batch))
  )

(defn deallocate [line batch]
  batch

  )

(defn error! [message & [data e]]
  (throw (ex-info message (or data {}) e)))

