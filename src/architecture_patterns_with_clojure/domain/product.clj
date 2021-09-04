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
                    (add-event product (events/out-of-stock line))
                    ))))

(defn- available-quantity-neg? [batch] (< (batch/available-quantity batch) 0))

(defn change_batch_quantity [product {:keys [ref quantity]}]
  (let [updated-batch (-> (first (filter #(= (:ref %) ref) (:batches product))) (assoc :quantity quantity))]
    (loop [updated-product (change-batch product updated-batch)
           updated-batch updated-batch]
      (if (available-quantity-neg? updated-batch)
        (let [line (last (:allocations updated-batch))
              new-batch (batch/deallocate updated-batch line)]
          (recur
            (-> (change-batch updated-product new-batch) (add-event (events/deallocated line)))
            new-batch))
        updated-product))))
