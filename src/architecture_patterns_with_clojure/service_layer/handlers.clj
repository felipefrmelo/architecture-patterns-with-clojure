(ns architecture-patterns-with-clojure.service-layer.handlers
  (:require [architecture-patterns-with-clojure.domain.order :as model]
            [architecture-patterns-with-clojure.domain.product :as product]
            [architecture-patterns-with-clojure.util.exception :refer [make-ex-info]]
            [architecture-patterns-with-clojure.adapters.repository :as repository]
            [architecture-patterns-with-clojure.domain.batch :as batch]
            [architecture-patterns-with-clojure.service-layer.message-bus :as message-bus]
            [architecture-patterns-with-clojure.domain.events :as events])
  (:import (architecture_patterns_with_clojure.domain.events
             OutOfStock BatchCreated AllocationRequired BatchQuantityChanged)))

(defn invalid-sku [line]
  (make-ex-info {:message (str "Invalid sku " (:sku line))
                 :data    [line]}))


(defn allocate [event repo]
  (let [line (model/new-order-line event)
        product (repository/get-by-sku repo (:sku event))]
    (when-not product (throw (invalid-sku line)))
    (let [p (product/allocate product line)]
      (repository/save repo p)
      (message-bus/handle (:events p)))
    ))

(defn add-batch [event repo]
  (let [sku (:sku event)
        product (repository/get-by-sku repo sku)
        batches (if product
                  (conj (:batches product) (batch/new-batch event))
                  [(batch/new-batch event)])]
    (repository/save repo (product/new-product {:sku sku :batches batches})))
  )

(defn change-batch-quantity [event repo]
  (let [product (repository/get-by-ref repo (:ref event))]
    (let [p (product/change_batch_quantity product event)]
      (repository/save repo p)
      (message-bus/handle (:events p) repo))
    ))


(defn send-out-of-stock-notification [event]
  (println (str "Out of stock for " (:sku event))))


(defmethod message-bus/handlers OutOfStock [_] [send-out-of-stock-notification])
(defmethod message-bus/handlers BatchCreated [_] [add-batch])
(defmethod message-bus/handlers AllocationRequired [_] [allocate])
(defmethod message-bus/handlers BatchQuantityChanged [_] [change-batch-quantity])
(defmethod message-bus/handlers :default [_] [(fn [x & dep] (println "can't handle with" x))])
