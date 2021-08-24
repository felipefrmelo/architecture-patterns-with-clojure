(ns architecture-patterns-with-clojure.unit.batches_test
  (:require [architecture-patterns-with-clojure.domain.order :as order]
            [architecture-patterns-with-clojure.domain.batch :as batch]
            [architecture-patterns-with-clojure.util.date :as date]
            [clojure.test :refer [deftest is testing]]
            [matcher-combinators.test :refer [match?]]))

(def today (date/now))
(def default-sku "SMALL-TABLE")
(defn make-batch-and-line [sku batch-qty line-qty]
  [(batch/new-batch {:ref "batch-001" :sku sku :quantity batch-qty :eta today})
   (order/new-order-line {:order_id "order-123" :sku sku :quantity line-qty})]
  )



(deftest test-batches
  (testing "can allocate if available is greater than required"
    (let [[large_batch small_line] (make-batch-and-line default-sku 20 2)]
      (is (match? true? (batch/can-allocate? large_batch small_line)))
      ))

  (testing "cannot allocate if available is smaller than required"
    (let [[small_batch large_line] (make-batch-and-line default-sku 2 20)]
      (is (match? false? (batch/can-allocate? small_batch large_line)))
      ))

  (testing "can allocate if available is equal to required"
    (let [[batch line] (make-batch-and-line default-sku 20 20)]
      (is (match? true? (batch/can-allocate? batch line)))
      ))

  (testing "cannot allocate if skus do not match"
    (let [[batch line] (make-batch-and-line default-sku 20 2)
          different-sku-li (assoc line :sku "DIFFERENT-SKU")]
      (is (match? false? (batch/can-allocate? batch different-sku-li)))
      ))

  (testing "allocating is idempotent"
    (let [[batch line] (make-batch-and-line default-sku 20 2)]
      (is (match? 18
                  (-> batch (batch/allocate line) (batch/allocate line) (batch/available-quantity))))
      ))

  (testing "test deallocate"
    (let [[batch line] (make-batch-and-line default-sku 20 2)]
      (is (match? 20 (-> batch (batch/allocate line) (batch/deallocate line) (batch/available-quantity))))
      ))

  (testing "can only deallocate allocated lines"
    (let [[batch unallocated_line] (make-batch-and-line default-sku 20 2)]
      (is (match? 20 (-> (batch/deallocate batch unallocated_line) (batch/available-quantity))))
      ))

  ;(testing "test deallocate one"
  ;  (let [[batch line] (make-batch-and-line default-sku 20 2)]
  ;    (is (match? line (-> (batch/allocate batch line) (batch/deallocate-one))))
  ;    ))

  )
