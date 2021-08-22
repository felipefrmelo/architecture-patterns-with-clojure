(ns architecture-patterns-with-clojure.service-layer.service
  (:require [architecture-patterns-with-clojure.domain.order :as model]
            [architecture-patterns-with-clojure.domain.product :as product]
            [architecture-patterns-with-clojure.util.exception :refer [make-ex-info]]
            [architecture-patterns-with-clojure.adapters.repository :as repository]
            [architecture-patterns-with-clojure.domain.batch :as batch]))

(defn invalid-sku [line]
  (make-ex-info {:message (str "Invalid sku " (:sku line))
                 :data    [line]}))


(defn allocate [{:keys [order_id sku quantity]} repo]
  (let [line (model/new-order-line {:order_id order_id :sku sku :quantity quantity})
        product (repository/get-by-sku repo sku)]
    (when-not product (throw (invalid-sku line)))
    (->> (product/allocate product line)
         (repository/save repo))
    ))

(defn add-batch [repo batch]
  (let [sku (:sku batch)
        product (repository/get-by-sku repo sku)
        batches (if product
                  (conj (:batches product) (batch/new-batch batch))
                  [(batch/new-batch batch)])]
    (repository/save repo (product/new-product {:sku sku :batches batches})))
  )