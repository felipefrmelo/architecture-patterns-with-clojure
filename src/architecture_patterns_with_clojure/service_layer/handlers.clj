(ns architecture-patterns-with-clojure.service-layer.handlers
  (:require [architecture-patterns-with-clojure.adapters.repository :as repository]
            [architecture-patterns-with-clojure.domain.batch :as batch]
            [architecture-patterns-with-clojure.domain.commands :as commands]
            [architecture-patterns-with-clojure.domain.order :as model]
            [architecture-patterns-with-clojure.domain.product :as product]
            [architecture-patterns-with-clojure.adapters.event-publisher
             :as
             event-publisher]
            [architecture-patterns-with-clojure.service-layer.message-bus
             :as
             message-bus]
            [architecture-patterns-with-clojure.util.exception
             :refer
             [make-ex-info]]))

(defn invalid-sku [line]
  (make-ex-info {:message (str "Invalid sku " (:sku line))
                 :data    [line]}))

(ns-unmap *ns* 'dispatch!)
(defmulti dispatch! (fn [command & deps] (:type command)))


(defn allocate [command repo]
  (let [line (model/new-order-line command)
        product (repository/get-by-sku repo (:sku command))]
    (when-not product (throw (invalid-sku line)))
    (let [p (product/allocate product line)]
      (repository/save repo (assoc  p :events []))
      (run! message-bus/emit! (:events p))
      )
    ))


(defn add-batch [command repo]
  (let [sku (:sku command)
        product (repository/get-by-sku repo sku)
        batches (if product
                  (conj (:batches product) (batch/new-batch command))
                  [(batch/new-batch command)])]
    (repository/save repo (product/new-product {:sku sku :batches batches})))
  )


(defn change-batch-quantity [command repo]
  (let [product (repository/get-by-ref repo (:ref command))
        p (product/change_batch_quantity product command)]
    (repository/save repo (assoc  p :events []))

    (run! message-bus/emit! (:events p))))

(defn reallocate [command repo]

  (dispatch! (commands/allocate command) :repo repo))

(defn send-out-of-stock-notification [event send-notification]
  (println (str "Out of stock for " (:sku event)))
  (send-notification (str "Out of stock for " (:sku event)))
  )


(defn publish-allocated-event [event pubsub]
 
  (event-publisher/producer pubsub "line_allocated" event)
  )

  
(def start (fn [repo pubsub send-notification]
             (message-bus/subscribe! :out-of-stock #(send-out-of-stock-notification % send-notification))
             (message-bus/subscribe! :deallocated #(reallocate % repo))
             (message-bus/subscribe! :allocated #(publish-allocated-event % pubsub))))

(defn stop [] (clojure.core.async/unsub-all message-bus/event-bus))

(defmethod dispatch! :create-batch [command & {:keys [repo]}] (add-batch (:payload command) repo))
(defmethod dispatch! :allocate [command & {:keys [repo]}] (allocate (:payload command) repo))
(defmethod dispatch! :change-batch-quantity [command & {:keys [repo]}] (change-batch-quantity (:payload command) repo))

(defn make-dispatch [repo pubsub send-notification]
  (start repo pubsub send-notification)
  (fn [command] (dispatch! command :repo repo)))




