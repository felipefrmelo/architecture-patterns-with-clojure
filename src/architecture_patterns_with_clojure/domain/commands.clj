(ns architecture-patterns-with-clojure.domain.commands)

(defn command [type payload]
  {:type type :payload payload})

(defn make-command [type keys]
  (fn [payload] (command type (select-keys payload keys))))


(def allocate (make-command :allocate [:order_id :sku :quantity]))
(def create-batch (make-command :create-batch [:sku :ref :quantity :eta]))
(def change-batch-quantity (make-command :change-batch-quantity [:ref :quantity]))




