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
(conj)
(defn new-product [{:keys [sku batches]}]
  (map->Product {:sku     sku
                 :batches batches
                 :events  []}))


(defn- batches-available [batches line]
  (filter #(batch/can-allocate? % line) batches))

(defn- get-prefer-batch [batches]
  (apply min-key #(date/to-inst-ms (:eta %)) batches))

(defn allocate ([product line]
                (try
                  (let [batches (:batches product)
                        batch-allocated (-> batches
                                            (batches-available line)
                                            (get-prefer-batch)
                                            (batch/allocate line))
                        updated-batches (map #(if (= (update % :allocations conj line) batch-allocated)
                                                batch-allocated
                                                %) batches)]
                    (new-product (assoc product :batches updated-batches)))
                  (catch ArityException e
                    (update product :events conj (events/->OutOfStock (:sku line)))
                    ;(throw (out-of-stock line))
                    ))))


