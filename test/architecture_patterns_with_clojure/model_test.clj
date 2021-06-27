(ns architecture-patterns-with-clojure.model-test
  (:require [clojure.test :refer :all]
            [matcher-combinators.test :refer [match? thrown-match?]]
            [architecture-patterns-with-clojure.model :as model]
            [architecture-patterns-with-clojure.util.date :as date]
            )
  (:import (clojure.lang ExceptionInfo)))

(def default-sku "SMALL-TABLE")

(def today (date/now))
(def tomorrow (date/plus today 1 :days))
(def later (date/plus today 10 :days))

(def earliest (model/new-batch {:ref "batch-001-earliest" :sku default-sku :quantity 100 :eta today}))
(def medium (model/new-batch {:ref "batch-002-medium" :sku default-sku :quantity 100 :eta tomorrow}))
(def latest (model/new-batch {:ref "batch-003-latest" :sku default-sku :quantity 100 :eta later}))
(def default-line (model/new-order-line {:order-id "123" :sku default-sku :quantity 10}))

(defn make-batch-and-line [sku batch-qty line-qty]
  [(model/new-batch {:ref "batch-001" :sku sku :quantity batch-qty :eta today})
   (model/new-order-line {:order-id "order-123" :sku sku :quantity line-qty})]
  )


(deftest test-allocate
  (testing "allocating to a batch reduces the available quantity"
    (let [[batch line] (make-batch-and-line default-sku 20 2)]
      (is (match? 18
                  (:available-quantity (model/allocate line batch))))
      ))

  (testing "can allocate if available is greater than required"
    (let [[large_batch small_line] (make-batch-and-line default-sku 20 2)]
      (is (match? true? (model/can-allocate? small_line large_batch)))
      ))

  (testing "cannot allocate if available is smaller than required"
    (let [[small_batch large_line] (make-batch-and-line default-sku 2 20)]
      (is (match? false? (model/can-allocate? large_line small_batch)))
      ))

  (testing "can allocate if available is equal to required"
    (let [[batch line] (make-batch-and-line default-sku 20 20)]
      (is (match? true? (model/can-allocate? line batch)))
      ))

  (testing "cannot allocate if skus do not match"

    (let [[batch line] (make-batch-and-line default-sku 20 2)
          different-sku-li (assoc line :sku "DIFFERENT-SKU")]
      (is (match? false? (model/can-allocate? different-sku-li batch)))
      ))
  (testing "can only deallocate allocated_lines"
    (let [[batch unallocated_line] (make-batch-and-line default-sku 20 2)]
      (is (match? 20 (:available-quantity (model/deallocate unallocated_line batch))))
      ))

  (testing "allocating is idempotent"
    (let [[batch line] (make-batch-and-line default-sku 20 2)]
      (is (match? {:available-quantity 18}
                  (->> batch (model/allocate line) (model/allocate line))))
      ))

  (testing "prefers current stock batches to shipments"
    (let [line (model/new-order-line {:order-id "123" :sku default-sku :quantity 10})]
      (is (match? {:ref "batch-001-earliest" :available-quantity 90} (model/allocate line earliest medium)))
      ))

  (testing "prefers earlier batches"
    (let [line (model/new-order-line {:order-id "123" :sku default-sku :quantity 10})]
      (is (match? {:ref "batch-001-earliest" :available-quantity 90} (model/allocate line medium earliest latest)))
      ))

  (testing "prefers earlier batches available"
    (let [line (model/new-order-line {:order-id "123" :sku default-sku :quantity 10})]
      (is (match? {:ref "batch-002-medium" :available-quantity 90} (model/allocate line
                                                                                   (assoc earliest :available-quantity 0)
                                                                                   medium latest)))
      ))

  (testing "raises out of stock exception if cannot allocate"
    (let [[batch line1] (make-batch-and-line default-sku 10 10)
          line2 (model/new-order-line {:order-id "new-order" :sku default-sku :quantity 1})]
      (is (thrown-match? ExceptionInfo (ex-data (model/out-of-stock line2))
                         (->> batch (model/allocate line1) (model/allocate line2))))
      ))

  (testing "raises out of stock exception if cannot allocate in multiples batches"
    (let [line (model/new-order-line {:order-id "123" :sku default-sku :quantity 999999})]
      (is (thrown-match? ExceptionInfo (ex-data (model/out-of-stock line)) (model/allocate line earliest medium latest)))
      ))

  )



