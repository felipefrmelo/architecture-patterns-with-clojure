(ns architecture-patterns-with-clojure.repository-test

  (:require [clojure.test :refer :all]
            [matcher-combinators.test :refer [match? thrown-match?]]
            [architecture-patterns-with-clojure.repository :as repo]
            [architecture-patterns-with-clojure.model :as model]
            [architecture-patterns-with-clojure.database :as database]
            [clojure.java.jdbc :as jdbc]
            [architecture-patterns-with-clojure.fixtures :as fixtures]
            [architecture-patterns-with-clojure.util.date :as date]))


(use-fixtures :each fixtures/db-fixture)

(def today (date/now))

(defn insert-order-line
  ([db] (insert-order-line db "batch1"))
  ([db order-linde-id]
   (-> (jdbc/insert! db :order_lines {:order_id order-linde-id :sku "GENERIC-SOFA" :quantity 12})
       first :id))
  )

(defn insert-batch [db batch-id]
  (-> (jdbc/insert! db :batches {:ref batch-id :sku "GENERIC-SOFA" :quantity 100 :eta today})
      first :id))

(defn insert-allocation
  [db order-linde-id batch-id]
  (jdbc/insert! db :allocations {:order_line_id order-linde-id :batch_id batch-id}))

(deftest test-repository

  (testing "repository can save a batch"
    (let [batch
          (model/new-batch {:ref "batch1" :sku "RUSTY-SOAPDISH" :quantity 100})
          sql-repo (repo/new-sql-repo database/db)]
      (repo/add sql-repo batch)
      (is (match? {:ref "batch1" :sku "RUSTY-SOAPDISH" :quantity 100}
                  (first (jdbc/query database/db ["select  ref, sku, quantity from batches"]))))
      )
    )

  )

(deftest test-repository
  (testing "repository can retrieve a batch with allocations"
    (let [order-line-id (insert-order-line database/db)
          order-line-id2 (insert-order-line database/db "batch2")
          batch-id (insert-batch database/db "batch1")
          _ (insert-batch database/db "batch2")
          _ (insert-allocation database/db order-line-id batch-id)
          _ (insert-allocation database/db order-line-id2 batch-id)
          sql-repo (repo/new-sql-repo database/db)
          retrieved (repo/get-by-ref sql-repo "batch1")
          expected (model/new-batch {:ref "batch1" :sku "GENERIC-SOFA" :quantity 100 :eta today})]

      (is (match? (assoc expected :allocations
                                  #{{:order_id "batch1", :quantity 12, :sku "GENERIC-SOFA"}
                                    {:order_id "batch2", :quantity 12, :sku "GENERIC-SOFA"}}) retrieved))

      )
    ))

