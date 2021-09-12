(ns architecture-patterns-with-clojure.domain.events)

;(ns-unmap *ns* 'Event)
;(defprotocol MyProtocol
;  (-my-fn [this args]))
;
;(defn my-fn [this & args] (-my-fn this args))

(defn event [type payload]
  {:type type :payload payload})

(defn make-event [type keys] (fn [payload] (event type (select-keys payload keys))))


(def deallocated (make-event :deallocated [:order_id :sku :quantity]))
(def out-of-stock (make-event :out-of-stock [:sku]))
(def allocated (make-event :allocated [:order_id :sku :quantity :batchref]))





