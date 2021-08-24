(ns architecture-patterns-with-clojure.unit.handler-test
  (:require [architecture-patterns-with-clojure.adapters.repository :as repository]
            [architecture-patterns-with-clojure.service-layer.handlers :as handlers]
            [clojure.test :refer [deftest is testing]]
            [matcher-combinators.test :refer [match?, thrown-match?]]
            [architecture-patterns-with-clojure.util.date :as date]
            [architecture-patterns-with-clojure.service-layer.message-bus :as message-bus]
            [architecture-patterns-with-clojure.domain.events :as events]
            [architecture-patterns-with-clojure.domain.batch :as batch])
  (:import (clojure.lang ExceptionInfo)))



(defrecord FakeRepository [products]
  repository/AbstractRepository
  (save [this product] (swap! products assoc (:sku product) product))
  (get-by-sku [this sku] (get @products sku))
  (get-by-ref [this ref] (some (fn [prod] (when (some #(= (:ref %) ref) (:batches prod)) prod)) (vals @products)))
  )

(defn new-fake-repo []
  (->FakeRepository (atom {})))


(defn make-batch [& {:keys [ref qty sku eta]
                     :or   {ref "b1" qty 100 sku "SMALL-TABLE" eta (date/now)}}]
  {:ref ref :sku sku :quantity qty :eta eta})

(defn make-line [& {:keys [order_id qty sku]
                    :or   {order_id "o1" qty 10 sku "SMALL-TABLE"}}]
  {:order_id order_id :sku sku :quantity qty})

(deftest add-batch

  (testing "add batch for a new product"
    (let [repo (new-fake-repo)
          batch (make-batch :sku "SMALL-TABLE")]
      (message-bus/handle [(events/make-batch-created batch)] repo)

      (is (match? {:sku "SMALL-TABLE"} (repository/get-by-sku repo "SMALL-TABLE")))))

  (testing "add batch for a existing product"
    (let [repo (new-fake-repo)
          batch1 (make-batch :ref "b1" :sku "SMALL-TABLE")
          batch2 (make-batch :ref "b2" :sku "SMALL-TABLE")]
      (message-bus/handle [(events/make-batch-created batch1)
                           (events/make-batch-created batch2)] repo)

      (is (match? [batch1 batch2] (:batches (repository/get-by-sku repo "SMALL-TABLE"))))))
  )


(deftest allocate

  (testing "allocation"
    (let [batch (make-batch :ref "b1" :sku "SMALL-TABLE")
          line (make-line)
          repo (new-fake-repo)]
      (message-bus/handle [(events/make-batch-created batch)
                           (events/make-allocation-required line)] repo)
      (is (match? #{line} (-> (repository/get-by-sku repo "SMALL-TABLE")
                              :batches
                              first
                              :allocations)))))

  (testing "error invalid sku"
    (let [batch (make-batch :ref "b1" :sku "AREALSKU")
          line (make-line :sku "NONEXISTENTSKU")
          repo (new-fake-repo)]

      (is (thrown-match?
            ExceptionInfo (ex-data (handlers/invalid-sku line))
            (message-bus/handle [(events/make-batch-created batch)
                                 (events/make-allocation-required line)] repo)))))

  (testing "sends email on out of stock error"
    (let [batch (make-batch :qty 10)
          line (make-line :qty 11)
          repo (new-fake-repo)
          notificationsIsCall (atom false)
          ]
      (with-redefs [handlers/send-out-of-stock-notification (constantly (swap! notificationsIsCall not))]
        (message-bus/handle [(events/make-batch-created batch)
                             (events/make-allocation-required line)] repo)
        (is (true? @notificationsIsCall)))))
  )


(deftest change-batch-quantity

  (testing "changes available quantity"
    (let [batch (make-batch :ref "b1" :sku "SMALL-TABLE" :qty 100)
          repo (new-fake-repo)
          get-available-quantity #(-> (repository/get-by-sku repo "SMALL-TABLE")
                                      :batches
                                      first
                                      (batch/available-quantity))]
      (message-bus/handle [(events/make-batch-created batch)] repo)
      (is (match? 100 (get-available-quantity)))
      (message-bus/handle [(events/make-batch-quantity-changed {:ref "b1" :quantity 50})] repo)
      (is (match? 50 (get-available-quantity)))
      ))

  (testing "reallocates if necessary"
    (let [batch1 (make-batch :ref "b1" :sku "SMALL-TABLE" :qty 50)
          batch2 (make-batch :ref "b2" :sku "SMALL-TABLE" :qty 50 :eta (date/plus (date/now) 1 date/DAYS))
          line1 (make-line :order_id "o1" :sku "SMALL-TABLE" :qty 20)
          line2 (make-line :order_id "o2" :sku "SMALL-TABLE" :qty 20)
          events [(events/make-batch-created batch1)
                  (events/make-batch-created batch2)
                  (events/make-allocation-required line1)
                  (events/make-allocation-required line2)]
          repo (new-fake-repo)
          get-available-quantities #(->> (repository/get-by-sku repo "SMALL-TABLE")
                                         :batches
                                         (map batch/available-quantity)
                                         )]
      (message-bus/handle events repo)
      (is (match? [10 50] (get-available-quantities)))
      (message-bus/handle [(events/make-batch-quantity-changed {:ref "b1" :quantity 25})] repo)
      (is (match? [30 5] (get-available-quantities)))
      ))

  )


