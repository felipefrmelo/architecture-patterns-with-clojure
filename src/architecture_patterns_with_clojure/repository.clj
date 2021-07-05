(ns architecture-patterns-with-clojure.repository
  (:require [clojure.java.jdbc :as jdbc]
            [architecture-patterns-with-clojure.util.date :as date]
            [architecture-patterns-with-clojure.model :as model]))


(defprotocol AbstractRepository
  (add [this batch])
  (get-by-ref [this ref])
  )


(defn mount-batch [{:keys [order_id order_quantity order_sku] :as batch}]
  (-> (model/new-batch batch)
      (assoc :allocations #{(model/new-order-line {:order_id order_id :quantity order_quantity :sku order_sku})})
      (update :eta date/from-db)
      )
  )

(defn group-allocations [batches]
  (reduce (fn [result current_batch] (update result :allocations into (:allocations current_batch)))
          batches))


(defrecord SqlRepository [db]
  AbstractRepository
  (add [this batch]
    (let [{:keys [allocations]} batch
          db-spec (:db this)
          new-batch (jdbc/insert! db-spec :batches (dissoc batch :allocations))
          new-lines (pmap #(jdbc/insert! db-spec :order_lines %) allocations)
          allocations (map
                        (fn [l] {:order_line_id (:id (first l)) :batch_id (:id (first new-batch))})
                        new-lines)]
      (jdbc/insert-multi! db-spec :allocations allocations))
    )
  (get-by-ref [this ref]
    (let [db-spec (:db this)
          batches (jdbc/query db-spec [
                                       "select b.quantity , b.ref, b.sku ,b.eta,
                                        ol.sku as order_sku, ol.quantity as order_quantity, ol.order_id
                                       from batches  as b
                                       left join allocations a on b.id = a.batch_id
                                       inner join order_lines ol on ol.id = a.order_line_id
                                       where b.ref = ? " ref
                                       ] {:row-fn        mount-batch
                                          :result-set-fn group-allocations})]
      batches
      ))
  )

(defn new-sql-repo [db]
  (SqlRepository. db))

