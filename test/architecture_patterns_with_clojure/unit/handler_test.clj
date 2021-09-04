(ns architecture-patterns-with-clojure.unit.handler-test
  (:require [architecture-patterns-with-clojure.adapters.repository :as repository :refer [new-in-memory-repository]]
            [architecture-patterns-with-clojure.service-layer.handlers :as handlers]
            [clojure.test :refer [deftest is testing use-fixtures]]
            [matcher-combinators.test :refer [match?, thrown-match?]]
            [architecture-patterns-with-clojure.util.date :as date]
            [architecture-patterns-with-clojure.domain.batch :as batch]
            [architecture-patterns-with-clojure.domain.commands :as commands]
            [architecture-patterns-with-clojure.service-layer.message-bus :as message-bus]
            )
  (:import (clojure.lang ExceptionInfo))
  )

(use-fixtures :each (fn [f] (f) (handlers/stop)))



(defn make-batch [& {:keys [ref qty sku eta]
                     :or   {ref "b1" qty 100 sku "SMALL-TABLE" eta (date/now)}}]
  {:ref ref :sku sku :quantity qty :eta eta})

(defn make-line [& {:keys [order_id qty sku]
                    :or   {order_id "o1" qty 10 sku "SMALL-TABLE"}}]
  {:order_id order_id :sku sku :quantity qty})

(deftest add-batch

  (testing "add batch for a new product"
    (let [repo (new-in-memory-repository)
          batch (make-batch :sku "SMALL-TABLE")
          ]
      (handlers/dispatch! (commands/create-batch batch) :repo repo)

      (is (match? {:sku "SMALL-TABLE"} (repository/get-by-sku repo "SMALL-TABLE")))))

  (testing "add batch for a existing product"
    (let [repo (new-in-memory-repository)
          batch1 (make-batch :ref "b1" :sku "SMALL-TABLE")
          batch2 (make-batch :ref "b2" :sku "SMALL-TABLE")]
      (handlers/dispatch! (commands/create-batch batch1) :repo repo)
      (handlers/dispatch! (commands/create-batch batch2) :repo repo)

      (is (match? [batch1 batch2] (:batches (repository/get-by-sku repo "SMALL-TABLE"))))))

  )


(deftest allocate

  (testing "allocation"
    (let [batch (make-batch :ref "b1" :sku "SMALL-TABLE")
          line (make-line)
          repo (new-in-memory-repository)]
      (handlers/dispatch! (commands/create-batch batch) :repo repo)
      (handlers/dispatch! (commands/allocate line) :repo repo)

      (is (match? #{line} (-> (repository/get-by-sku repo "SMALL-TABLE")
                              :batches
                              first
                              :allocations)))))

  (testing "error invalid sku"
    (let [batch (make-batch :ref "b1" :sku "AREALSKU")
          line (make-line :sku "NONEXISTENTSKU")
          repo (new-in-memory-repository)]
      (handlers/dispatch! (commands/create-batch batch) :repo repo)
      (is (thrown-match?
            ExceptionInfo (ex-data (handlers/invalid-sku line))
            (handlers/dispatch! (commands/allocate line) :repo repo)))))

  (testing "sends email on out of stock error"
    (let [batch (make-batch :qty 10)
          line (make-line :qty 11)
          repo (new-in-memory-repository)
          notificationsIsCall (atom false)
          ]


      (with-redefs [handlers/send-out-of-stock-notification
                    (fn [event] (swap! notificationsIsCall not))]
        (handlers/start repo)
        (handlers/dispatch! (commands/create-batch batch) :repo repo)
        (handlers/dispatch! (commands/allocate line) :repo repo)
        (Thread/sleep 100)

        (is (true? @notificationsIsCall)))))
  )


(deftest change-batch-quantity

  (testing "changes available quantity"
    (let [batch (make-batch :ref "b1" :sku "SMALL-TABLE" :qty 100)
          repo (new-in-memory-repository)
          get-available-quantity #(-> (repository/get-by-sku repo "SMALL-TABLE")
                                      :batches
                                      first
                                      (batch/available-quantity))]
      (handlers/dispatch! (commands/create-batch batch) :repo repo)
      (is (match? 100 (get-available-quantity)))
      (handlers/dispatch! (commands/change-batch-quantity {:ref "b1" :quantity 50}) :repo repo)
      (is (match? 50 (get-available-quantity)))
      ))


  (testing "reallocates if necessary"
    (let [batch1 (make-batch :ref "b1" :sku "SMALL-TABLE" :qty 50)
          batch2 (make-batch :ref "b2" :sku "SMALL-TABLE" :qty 50 :eta (date/plus (date/now) 1 date/DAYS))
          line1 (make-line :order_id "o1" :sku "SMALL-TABLE" :qty 20)
          line2 (make-line :order_id "o2" :sku "SMALL-TABLE" :qty 20)
          cmd [(commands/create-batch batch1)
               (commands/create-batch batch2)
               (commands/allocate line1)
               (commands/allocate line2)]
          repo (new-in-memory-repository)
          get-available-quantities #(->> (repository/get-by-sku repo "SMALL-TABLE")
                                         :batches
                                         (map batch/available-quantity)
                                         )]
      (handlers/start repo)

      (run! #(handlers/dispatch! % :repo repo) cmd)
      (is (match? [10 50] (get-available-quantities)))
      (handlers/dispatch! (commands/change-batch-quantity {:ref "b1" :quantity 25}) :repo repo)
      (Thread/sleep 100)
      (is (match? [30 5] (get-available-quantities)))
      ))

  )


