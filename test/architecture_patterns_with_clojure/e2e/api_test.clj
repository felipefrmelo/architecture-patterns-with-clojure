(ns architecture-patterns-with-clojure.e2e.api-test
  (:require [architecture-patterns-with-clojure.adapters.event-publisher
             :as
             event-publisher]
            [architecture-patterns-with-clojure.adapters.repository :as repository]
            [architecture-patterns-with-clojure.e2e.helper-api
             :refer
             [get-allocation post-to-add-batch! post-to-allocate!]]
            [architecture-patterns-with-clojure.entrypoints.api :as api]
            [architecture-patterns-with-clojure.fixtures :as fixtures]
            [architecture-patterns-with-clojure.random-refs
             :refer
             [random-batchref random-orderid random-sku]]
            [architecture-patterns-with-clojure.service-layer.handlers :as handlers]
            [architecture-patterns-with-clojure.util.json :as json]
            [clojure.test :refer [deftest is use-fixtures]]
            [io.pedestal.http :as bootstrap]
            [matcher-combinators.test :refer [match?]]
            [architecture-patterns-with-clojure.views :as views]))

(use-fixtures :each fixtures/clean-db (fn [f] (f) (handlers/stop) ))

(def service
  (::bootstrap/service-fn (bootstrap/create-servlet
                           (api/service
                            (handlers/make-dispatch
                             (repository/new-mongo-repository)
                             (event-publisher/new-pubsub-dummy)
                             identity
                             views/insert-view)))))


(deftest happy-path-returns-201-and-allocated-batch
  (let [order_id (random-orderid)
        sku (random-sku)
        early-batch (random-batchref :name 1)
        later-batch (random-batchref :name 2)
        other-batch (random-batchref :name 3)]
    (post-to-add-batch! service early-batch sku 10 "2011-01-01T00:00")
    (post-to-add-batch! service later-batch sku 10 "2011-01-02T00:00")
    (post-to-add-batch! service other-batch sku 10 "2011-01-03T00:00")

    
    (post-to-allocate! service order_id sku 5)
    (is (= 1 1))
    (Thread/sleep 250)
    (is (match? {:sku sku :batchref early-batch} (-> (get-allocation service order_id)
                                                     :body
                                                     json/parse
                                                     first
                                                     (select-keys [:sku :batchref]))))))

(deftest unhappy-path-returns-400-and-error-message
  (let [order_id (random-orderid)
        unknown_sku (random-sku)
        ]
    (is (= (str "Invalid sku " unknown_sku)
           (-> (post-to-allocate! service order_id unknown_sku 10 400)
               :body json/parse :msg)))
    )
  )

