(ns architecture-patterns-with-clojure.adapters.repository
  (:require [clojure.java.jdbc :as jdbc]
            [architecture-patterns-with-clojure.util.date :as date]
            [architecture-patterns-with-clojure.domain.order :as order]
            [architecture-patterns-with-clojure.domain.batch :as batch]
            [architecture-patterns-with-clojure.domain.product :as product]
            [monger.core :as mg]
            [monger.collection :as mc])
  (:import (org.bson.types ObjectId) ))



(defprotocol AbstractRepository
  (save [this product])
  (get-by-sku [this sku])
  (get-by-ref [this ref])
  )


(defrecord InMemoryRepository [products]
  AbstractRepository
  (save [this product] (swap! products assoc (:sku product) product))
  (get-by-sku [this sku] (get @products sku))
  (get-by-ref [this ref] (some (fn [prod] (when (some #(= (:ref %) ref) (:batches prod)) prod)) (vals @products)))
  )

(defn new-in-memory-repository []
  (->InMemoryRepository (atom {})))


(defn batch-from-db [b]
  (-> b (update :eta date/from-db)
      (update :allocations (comp set #(map order/new-order-line %)))
      (batch/new-batch)))

(defn product-from-db [p]
  (when p (-> p (update :batches #(mapv batch-from-db %))
              (update :events #(mapv (fn [e] (update e :type keyword)) %))
              (product/new-product))))

(defrecord MongoRepository [db coll]
  AbstractRepository
  (save [this product] (mc/update db coll {:sku (:sku product)}
                                  (update product :batches #(mapv (fn[b] (update b :eta date/to-db) ) %)) {:upsert true}))
  (get-by-sku [this sku] (-> 
                          (mc/find-one-as-map db  coll {:sku sku})
                          (product-from-db)
                          ))
  (get-by-ref [this ref] (-> (mc/find-one-as-map db coll {"batches.ref" ref})
                             product-from-db)))

(def db (let [conn (mg/connect)]
              (mg/get-db conn "monger-test")))


(defn new-mongo-repository []
  (->MongoRepository db "product"))
