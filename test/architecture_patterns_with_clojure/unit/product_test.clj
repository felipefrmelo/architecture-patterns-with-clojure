(ns architecture-patterns-with-clojure.unit.product-test
  (:require [clojure.test :refer [deftest is testing]]
            [matcher-combinators.test :refer [match? thrown-match?]]
            [architecture-patterns-with-clojure.domain.order :as order]
            [architecture-patterns-with-clojure.domain.product :as product]
            [architecture-patterns-with-clojure.domain.batch :as batch]
            [architecture-patterns-with-clojure.util.date :as date]
            [architecture-patterns-with-clojure.domain.events :as events]))

(def default-sku "SMALL-TABLE")

(def today (date/now))
(def tomorrow (date/plus today 1 date/DAYS))
(def later (date/plus today 10 date/DAYS))


(def earliest (batch/new-batch {:ref "batch-001-earliest" :sku default-sku :quantity 100 :eta today}))
(def medium (batch/new-batch {:ref "batch-002-medium" :sku default-sku :quantity 100 :eta tomorrow}))
(def latest (batch/new-batch {:ref "batch-003-latest" :sku default-sku :quantity 100 :eta later}))

(defn make-batch-and-line [sku batch-qty line-qty]
  [(batch/new-batch {:ref "batch-001" :sku sku :quantity batch-qty :eta today})
   (order/new-order-line {:order_id "order-123" :sku sku :quantity line-qty})]
  )

(defn get-batch [product batch]
  (first (filter #(= (:ref %) (:ref batch)) (:batches product))))

(defn assertIfBatchIsAllocatedCorrect [product line batch]
  (is (match? (batch/allocate batch line)
              (-> (product/allocate product line)
                  (get-batch batch))))
  )

(deftest test-allocate
  (testing "allocating to a batch reduces the available quantity"
    (let [[batch line] (make-batch-and-line default-sku 20 2)
          product (product/new-product {:sku default-sku :batches [batch]})]
      (assertIfBatchIsAllocatedCorrect product line batch)
      ))


  (testing "prefers earlier batches"
    (let [line (order/new-order-line {:order_id "123" :sku default-sku :quantity 10})
          product (product/new-product {:sku default-sku :batches [medium earliest latest]})]
      (assertIfBatchIsAllocatedCorrect product line earliest)

      ))

  (testing "prefers earlier batches available"
    (let [[earliest line] (make-batch-and-line default-sku 9 10)
          product (product/new-product {:sku default-sku :batches [earliest medium latest]})]
      (assertIfBatchIsAllocatedCorrect product line medium)

      ))

  (testing "records out of stock event if cannot allocate"
    (let [[batch line] (make-batch-and-line default-sku 10 11)
          product (product/new-product {:sku default-sku :batches [batch]})]
      (is (match? (events/->OutOfStock default-sku)
                  (last (:events (product/allocate product line)))))
      ))
  (testing "raises out of stock exception if cannot allocate in multiples batches"
    (let [line (order/new-order-line {:order_id "123" :sku default-sku :quantity 101})
          product (product/new-product {:sku default-sku :batches [earliest medium latest]})]
      (is (match? (events/->OutOfStock default-sku)
                  (last (:events (product/allocate product line)))))
      ))
  )



