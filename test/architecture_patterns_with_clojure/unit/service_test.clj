(ns architecture-patterns-with-clojure.unit.service-test
  (:require [architecture-patterns-with-clojure.adapters.repository :as repository]
            [architecture-patterns-with-clojure.service-layer.service :as service]
            [clojure.test :refer [deftest is testing]]
            [matcher-combinators.test :refer [match?, thrown-match?]]
            [architecture-patterns-with-clojure.util.date :as date])
  (:import (clojure.lang ExceptionInfo)))


(defrecord FakeRepository [products]
  repository/AbstractRepository
  (save [this product] (swap! products assoc (:sku product) product))
  (get-by-sku [this sku] (get @products sku))
  )

(defn new-fake-repo []
  (->FakeRepository (atom {})))


(defn make-batch [& {:keys [ref qty sku eta]
                     :or   {ref "b1" qty 100 sku "SMALL-TABLE" eta (date/now)}}]
  {:ref ref :sku sku :quantity qty :eta eta})

(defn make-line [& {:keys [order_id qty sku]
                    :or   {order_id "o1" qty 10 sku "SMALL-TABLE"}}]
  {:order_id order_id :sku sku :quantity qty})

(deftest test-batches

  (testing "add batch for a new product"
    (let [repo (new-fake-repo)
          batch (make-batch :sku "SMALL-TABLE")]
      (service/add-batch repo batch)
      (is (match? {:sku "SMALL-TABLE"} (repository/get-by-sku repo "SMALL-TABLE"))))
    )

  (testing "add batch for a existing product"
    (let [repo (new-fake-repo)
          batch1 (make-batch :ref "b1" :sku "SMALL-TABLE")
          batch2 (make-batch :ref "b2" :sku "SMALL-TABLE")]
      (service/add-batch repo batch1)
      (service/add-batch repo batch2)
      (is (match? [batch1 batch2] (:batches (repository/get-by-sku repo "SMALL-TABLE")))))
    )

  (testing "allocation"
    (let [batch (make-batch :ref "b1" :sku "SMALL-TABLE")
          line (make-line)
          repo (new-fake-repo)]
      (service/add-batch repo batch)
      (service/allocate line repo)
      (is (match? #{line} (-> (repository/get-by-sku repo "SMALL-TABLE")
                      :batches
                      first
                      :allocations)))))


  (testing "error invalid sku"
    (let [batch (make-batch :ref "b1" :sku "AREALSKU")
          line (make-line :sku "NONEXISTENTSKU")
          repo (new-fake-repo)]
      (service/add-batch repo batch)
      (is (thrown-match?
            ExceptionInfo (ex-data (service/invalid-sku line))
            (service/allocate line repo)))))

  )
  

