(ns architecture-patterns-with-clojure.integration.repository-test

  (:require [clojure.test :refer [use-fixtures deftest testing is]]
            [matcher-combinators.test :refer [match?]]
            [architecture-patterns-with-clojure.adapters.repository :as repository]
            [architecture-patterns-with-clojure.domain.product :as product ]
            [architecture-patterns-with-clojure.domain.batch :as batch]
            [architecture-patterns-with-clojure.domain.order :as order]
            [architecture-patterns-with-clojure.fixtures :as fixtures]
            [architecture-patterns-with-clojure.util.date :as date]))

 
(use-fixtures :each fixtures/clean-db)

(def today (date/now))




(deftest test-repository-save
  (testing "repository can save a a product"
    (let [batch (batch/new-batch {:ref "batch1" :sku "RUSTY-SOAPDISH" :quantity 100})
          prod (product/new-product {:sku "RUSTY-SOAPDISH" :batches [batch]})
          mongo-repo (repository/new-mongo-repository)]
      (repository/save mongo-repo prod)
      (is (match? {:sku "RUSTY-SOAPDISH" :batches [(dissoc batch :eta)]}
                  (repository/get-by-sku mongo-repo "RUSTY-SOAPDISH" ))))))

(deftest test-repository-retrieve-by-sku
  (testing "repository can retrieve a batch by ref"
    (let [batch (batch/new-batch {:ref "batch1" :sku "RUSTY-SOAPDISH" :quantity 100})
          prod (product/new-product {:sku "RUSTY-SOAPDISH" :batches [batch]})
          mongo-repo (repository/new-mongo-repository)]
      (repository/save mongo-repo prod)
      (is (match? {:sku "RUSTY-SOAPDISH" :batches [(dissoc batch :eta)]}
                  (repository/get-by-ref mongo-repo "batch1" ))))))


(deftest test-repository-retrieve
  (testing "repository can retrieve a batch by sku and with allocations"
    (let [batch (batch/new-batch {:ref "batch1" :sku "RUSTY-SOAPDISH" :quantity 100})
          prod (product/new-product {:sku "RUSTY-SOAPDISH" :batches [batch]})
          ord1 (order/new-order-line {:order_id "order-1" :sku "RUSTY-SOAPDISH" :quantity 10 })
          ord2 (order/new-order-line {:order_id "order-2" :sku "RUSTY-SOAPDISH" :quantity 10 })
          p2 (-> prod (product/allocate ord1)
                                      (product/allocate ord2))
          mongo-repo (repository/new-mongo-repository)]
      (repository/save mongo-repo p2)
      (is (match? {:sku "RUSTY-SOAPDISH" :batches [{:allocations #{ord1 ord2}}]}
                  (repository/get-by-sku mongo-repo "RUSTY-SOAPDISH" ))))))


