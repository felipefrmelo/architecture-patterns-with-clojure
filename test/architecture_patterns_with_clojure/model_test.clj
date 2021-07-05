(ns architecture-patterns-with-clojure.model-test
  (:require [clojure.test :refer :all]
            [matcher-combinators.test :refer [match? thrown-match?]]
            [architecture-patterns-with-clojure.model :as model]
            [architecture-patterns-with-clojure.util.date :as date]
            )
  (:import (clojure.lang ExceptionInfo)))

(def default-sku "SMALL-TABLE")

(def today (date/now))
(def tomorrow (date/plus today 1 date/DAYS))
(def later (date/plus today 10 date/DAYS))

(def earliest (model/new-batch {:ref "batch-001-earliest" :sku default-sku :quantity 100 :eta today}))
(def medium (model/new-batch {:ref "batch-002-medium" :sku default-sku :quantity 100 :eta tomorrow}))
(def latest (model/new-batch {:ref "batch-003-latest" :sku default-sku :quantity 100 :eta later}))

(defn make-batch-and-line [sku batch-qty line-qty]
  [(model/new-batch {:ref "batch-001" :sku sku :quantity batch-qty :eta today})
   (model/new-order-line {:order_id "order-123" :sku sku :quantity line-qty})]
  )


(deftest test-allocate
  (testing "allocating to a batch reduces the available quantity"
    (let [[batch line] (make-batch-and-line default-sku 20 2)]
      (is (match? 18
                  (model/available-quantity (model/allocate line batch))))
      ))



  (testing "prefers current stock batches to shipments"
    (let [line (model/new-order-line {:order_id "123" :sku default-sku :quantity 10})]
      (is (match? {:ref "batch-001-earliest" :allocations #{line} } (model/allocate line earliest medium)))
      ))

  (testing "prefers earlier batches"
    (let [line (model/new-order-line {:order_id "123" :sku default-sku :quantity 10})]
      (is (match? {:ref "batch-001-earliest" :allocations #{line}} (model/allocate line medium earliest latest)))
      ))

  (testing "prefers earlier batches available"
    (let [line (model/new-order-line {:order_id "123" :sku default-sku :quantity 10})]
      (is (match? {:ref "batch-002-medium" } (model/allocate line
                                                                                   (assoc earliest :allocations #{{:quantity 100}})
                                                                                   medium latest)))
      ))

  (testing "raises out of stock exception if cannot allocate"
    (let [[batch line1] (make-batch-and-line default-sku 10 10)
          line2 (model/new-order-line {:order_id "new-order" :sku default-sku :quantity 1})]
      (is (thrown-match? ExceptionInfo (ex-data (model/out-of-stock line2))
                         (->> batch (model/allocate line1) (model/allocate line2))))
      ))

  (testing "raises out of stock exception if cannot allocate in multiples batches"
    (let [line (model/new-order-line {:order_id "123" :sku default-sku :quantity 999999})]
      (is (thrown-match? ExceptionInfo (ex-data (model/out-of-stock line)) (model/allocate line earliest medium latest)))
      ))

  )



