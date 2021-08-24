(ns architecture-patterns-with-clojure.domain.product
  (:require [architecture-patterns-with-clojure.util.exception :refer [make-ex-info]]
            [architecture-patterns-with-clojure.domain.batch :as batch]
            [architecture-patterns-with-clojure.util.date :as date]
            [architecture-patterns-with-clojure.domain.events :as events])
  (:import (clojure.lang ArityException)))

(defn out-of-stock [line]
  (make-ex-info {:message (str "Out of stock for sku " (:sku line))
                 :data    [line]}))


(defrecord Product [sku batches events])

(defn new-product [{:keys [sku batches]}]
  (map->Product {:sku     sku
                 :batches batches
                 :events  []}))


(defn- batches-available [batches line]
  (filter #(batch/can-allocate? % line) batches))

(defn- get-prefer-batch [batches]
  (apply min-key #(date/to-inst-ms (:eta %)) batches))

(defn- change-batch [product batch]
  (let [clean-batches (->> (:batches product) (filter #(not= (:ref %) (:ref batch))))]
    (new-product (assoc product :batches (conj clean-batches batch)))))

(defn- add-event [product event]
  (update product :events (if (map? event) conj concat) event)
  )

(defn allocate ([product line]
                (try
                  (let [batches (:batches product)
                        batch-allocated (-> batches
                                            (batches-available line)
                                            (get-prefer-batch)
                                            (batch/allocate line))]
                    (change-batch product batch-allocated))
                  (catch ArityException e
                    (add-event product (events/make-out-of-stock (:sku line)))
                    ))))

(defn- sum-lines [lines]
  (apply + (map :quantity lines)))

(defn- quantity-to-deallocate-is-greater-than-unavailable? [lines batch]
  (>= (sum-lines lines) (- (batch/available-quantity batch)))
  )

(defn change_batch_quantity [product {:keys [ref quantity]}]
  (let [batch (-> (first (filter #(= (:ref %) ref) (:batches product))) (assoc :quantity quantity))
        lines-to-deallocate (reduce
                              (fn [lines line]
                                (if (quantity-to-deallocate-is-greater-than-unavailable? lines batch)
                                  (reduced lines)
                                  (conj lines line))) [] (:allocations batch))]
    (-> product (change-batch (apply batch/deallocate batch lines-to-deallocate))
        (add-event (map events/make-allocation-required lines-to-deallocate)))))

(defn change_batch_quantity-recursive [product {:keys [ref quantity]}]
  (let [batch (-> (first (filter #(= (:ref %) ref) (:batches product))) (assoc :quantity quantity))]
    (loop [p (change-batch product batch)
           b batch]
      (if (< (batch/available-quantity b) 0)
        (let [line (last (:allocations b))
              new-batch (batch/deallocate b line)
              n-prod (-> (change-batch p new-batch) (add-event (events/make-allocation-required line)))]
          (recur n-prod new-batch))
        p))))
