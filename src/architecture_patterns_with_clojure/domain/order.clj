(ns architecture-patterns-with-clojure.domain.order
  (:require [architecture-patterns-with-clojure.util.date :as date]
            [architecture-patterns-with-clojure.util.exception :refer [make-ex-info]])
  )



(defrecord OrderLine [order_id sku quantity])

(defn new-order-line [{:keys [order_id sku quantity]}]
  (->OrderLine order_id sku quantity))













