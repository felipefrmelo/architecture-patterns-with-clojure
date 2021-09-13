(ns architecture-patterns-with-clojure.e2e.test-external-events
  (:require [architecture-patterns-with-clojure.adapters.event-publisher
             :as
             event-publisher]
            [architecture-patterns-with-clojure.adapters.repository :as repository]
            [architecture-patterns-with-clojure.e2e.helper-api
             :refer
             [post-to-add-batch! post-to-allocate!]]
            [architecture-patterns-with-clojure.entrypoints.api :as api]
            [architecture-patterns-with-clojure.entrypoints.event-consumer
             :as
             event-consumer]
            [architecture-patterns-with-clojure.random-refs :as random]
            [architecture-patterns-with-clojure.service-layer.handlers :as handlers]
            [clojure.test :refer [deftest is use-fixtures]]
            [io.pedestal.http :as bootstrap]
            [matcher-combinators.test :refer [match?]]
            [architecture-patterns-with-clojure.fixtures :as fixtures]))

(def service
  (::bootstrap/service-fn (bootstrap/create-servlet (api/service
                                                     (handlers/make-dispatch (repository/new-mongo-repository)
                                                                             (event-publisher/new-pubsub-dummy)
                                                                             identity
                                                                             identity)))))


(use-fixtures :each (fn [f]
                      (handlers/stop)
                      (with-open [conn (event-consumer/start)]
                        (f)))   (fn [f] (f) (handlers/stop))

  fixtures/clean-db)


(deftest test_change_batch_quantity_leading_to_reallocation
  (let [order_id (random/random-orderid)
        sku (random/random-sku)
        early-batch (random/random-batchref :name "early")
        later-batch (random/random-batchref :name "later")
        other-batch (random/random-batchref :name "other")
        payload (atom nil)
        handle (fn [_ _ pay]  (reset! payload pay))
        pubsub (event-publisher/new-pubsub-rmq)
        conn (event-publisher/consumer pubsub "line_allocated" handle)]
    (post-to-add-batch! service early-batch sku 10 "2022-01-01T00:00")
    (post-to-add-batch! service later-batch sku 10 "2023-01-02T02:00")
    (post-to-add-batch! service other-batch sku 10 "2024-01-03T03:00")
    (post-to-allocate!  service order_id sku 10)



    (Thread/sleep 100)
    (event-publisher/producer pubsub event-publisher/qname {:ref early-batch :quantity 5})
    (Thread/sleep 100)


    (is (match? {:batchref later-batch
                 :sku sku
                 :quantity 10
                 :order_id order_id} @payload))
    (.close conn)))







