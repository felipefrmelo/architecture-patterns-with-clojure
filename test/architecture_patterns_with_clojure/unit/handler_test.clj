(ns architecture-patterns-with-clojure.unit.handler-test
  (:require [architecture-patterns-with-clojure.adapters.repository :as repository :refer [new-in-memory-repository]]
            [architecture-patterns-with-clojure.service-layer.handlers :as handlers]
            [clojure.test :refer [deftest is testing use-fixtures]]
            [matcher-combinators.test :refer [match?, thrown-match?]]
            [architecture-patterns-with-clojure.util.date :as date]
            [architecture-patterns-with-clojure.domain.batch :as batch]
            [architecture-patterns-with-clojure.domain.commands :as commands]
            [architecture-patterns-with-clojure.adapters.event-publisher :as event-publisher]
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


(defn i-dont-know-the-name-yet
  ([] (i-dont-know-the-name-yet identity))
  ([send-notification]
   (handlers/stop)
   (let [repo (new-in-memory-repository)
         pubsub (event-publisher/new-pubsub-dummy)
         dispatch! (handlers/make-dispatch repo pubsub send-notification identity)
         ]  [dispatch! repo  pubsub])))



(deftest add-batch-for-a-new-product
  (let [batch (make-batch :sku "SMALL-TABLE")
        [dispatch! repo] (i-dont-know-the-name-yet)]
    
    (dispatch! (commands/create-batch batch))

    (is (match? {:sku "SMALL-TABLE"} (repository/get-by-sku repo "SMALL-TABLE")))))

(deftest add-batch-for-a-existing-product
  (let [[dispatch! repo] (i-dont-know-the-name-yet)
        
        batch1 (make-batch :ref "b1" :sku "SMALL-TABLE")
        batch2 (make-batch :ref "b2" :sku "SMALL-TABLE")]
    (dispatch! (commands/create-batch batch1))
    (dispatch! (commands/create-batch batch2))

    (is (match? [batch1 batch2] (:batches (repository/get-by-sku repo "SMALL-TABLE"))))))

  




(deftest allocation
  (let [batch (make-batch :ref "b1" :sku "SMALL-TABLE")
        line (make-line)
        [dispatch! repo] (i-dont-know-the-name-yet)]
    (dispatch! (commands/create-batch batch) )
    (dispatch! (commands/allocate line) )

    (is (match? #{line} (-> (repository/get-by-sku repo "SMALL-TABLE")
                            :batches
                            first
                            :allocations)))))

(deftest error-invalid-sku
  (let [batch (make-batch :ref "b1" :sku "AREALSKU")
        line (make-line :sku "NONEXISTENTSKU")
        [dispatch! repo] (i-dont-know-the-name-yet)]
    (dispatch! (commands/create-batch batch) )
    (is (thrown-match?
         ExceptionInfo (ex-data (handlers/invalid-sku line))
         (dispatch! (commands/allocate line) )))))

(deftest sends-email-on-out-of-stock-error
  (let [batch (make-batch :qty 10)
        line (make-line :qty 11)
        notificationsIsCall (atom false)
        [dispatch!] (i-dont-know-the-name-yet  (fn [notification] (reset! notificationsIsCall notification)))
        ]


    (dispatch! (commands/create-batch batch) )
    (dispatch! (commands/allocate line) )
    (Thread/sleep 100)

    (is (match? (str "Out of stock for " (:sku line)) @notificationsIsCall))))
  

(deftest change-batch-quantity

  (testing "changes available quantity"
    (let [batch (make-batch :ref "b1" :sku "SMALL-TABLE" :qty 100)
          [dispatch! repo] (i-dont-know-the-name-yet)
          get-available-quantity #(-> (repository/get-by-sku repo "SMALL-TABLE")
                                      :batches
                                      first
                                      (batch/available-quantity))]
      (dispatch! (commands/create-batch batch) )
      (is (match? 100 (get-available-quantity)))
      (dispatch! (commands/change-batch-quantity {:ref "b1" :quantity 50}))
      (is (match? 50 (get-available-quantity)))
      )))

(deftest reallocates-id-necessary
    
  (let [batch1 (make-batch :ref "b1" :sku "SMALL-TABLE" :qty 50)
        batch2 (make-batch :ref "b2" :sku "SMALL-TABLE" :qty 50 :eta (date/plus (date/now) 1 date/DAYS))
        line1 (make-line :order_id "o1" :sku "SMALL-TABLE" :qty 20)
        line2 (make-line :order_id "o2" :sku "SMALL-TABLE" :qty 20)
        cmd [(commands/create-batch batch1)
             (commands/create-batch batch2)
             (commands/allocate line1)
             (commands/allocate line2)]
        [dispatch! repo] (i-dont-know-the-name-yet)
        get-available-quantities #(->> (repository/get-by-sku repo "SMALL-TABLE")
                                       :batches
                                       (map batch/available-quantity)

                                       )]

    (run! #(dispatch! % ) cmd)
        
    (is (match? [10 50] (get-available-quantities)))
    (dispatch! (commands/change-batch-quantity {:ref "b1" :quantity 25}) )
    (Thread/sleep 100)
    (is (match? [30 5] (get-available-quantities)))
    ))




