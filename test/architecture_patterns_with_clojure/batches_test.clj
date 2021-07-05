(ns architecture-patterns-with-clojure.batches_test
  (:require [clojure.test :refer :all]
            [matcher-combinators.test :refer [match? thrown-match?]]
            [architecture-patterns-with-clojure.model :as model]
            [architecture-patterns-with-clojure.util.date :as date]))

(def today (date/now))
(def default-sku "SMALL-TABLE")
(defn make-batch-and-line [sku batch-qty line-qty]
  [(model/new-batch {:ref "batch-001" :sku sku :quantity batch-qty :eta today})
   (model/new-order-line {:order_id "order-123" :sku sku :quantity line-qty})]
  )

(deftest test-batches
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

  (testing "allocating is idempotent"
    (let [[batch line] (make-batch-and-line default-sku 20 2)]
      (is (match? 18
                  (->> batch (model/allocate line) (model/allocate line) (model/available-quantity))))
      ))

  (testing "test deallocate"
    (let [[batch line] (make-batch-and-line default-sku 20 2)]
      (is (match? 20 (->> batch (model/allocate line) (model/deallocate line) (model/available-quantity))))
      ))

  (testing "can only deallocate allocated lines"
    (let [[batch unallocated_line] (make-batch-and-line default-sku 20 2)]
      (is (match? 20 (-> (model/deallocate unallocated_line batch) (model/available-quantity))))
      ))

  )
