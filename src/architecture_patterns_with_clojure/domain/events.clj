(ns architecture-patterns-with-clojure.domain.events)

;(ns-unmap *ns* 'event)
;(defprotocol MyProtocol
;  (-my-fn [this args]))
;
;(defn my-fn [this & args] (-my-fn this args))


(defrecord OutOfStock [sku])
(defn make-out-of-stock [sku] (->OutOfStock sku))

(defrecord BatchCreated [ref sku quantity eta])
(defn make-batch-created [{:keys [ref sku quantity eta]}] (->BatchCreated ref sku quantity eta))

(defrecord AllocationRequired [order_id sku quantity])
(defn make-allocation-required [{:keys [order_id sku quantity]}] (->AllocationRequired order_id sku quantity))


(defrecord BatchQuantityChanged [ref quantity])
(defn make-batch-quantity-changed [{:keys [ref quantity]}] (->BatchQuantityChanged ref quantity ))