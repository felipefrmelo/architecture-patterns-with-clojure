(ns architecture-patterns-with-clojure.adapters.repository
  (:require [clojure.java.jdbc :as jdbc]
            [architecture-patterns-with-clojure.util.date :as date]
            [architecture-patterns-with-clojure.domain.order :as model]))



(defprotocol AbstractRepository
  (save [this product])
  (get-by-sku [this sku])
  (get-by-ref [this ref])
  )


;
;(defn mount-batch [{:keys [order_id order_quantity order_sku] :as batch}]
;  (-> (model/new-batch batch)
;      (assoc :allocations (if (some? order_id)
;                            #{(model/new-order-line {:order_id order_id :quantity order_quantity :sku order_sku})} #{}))
;      (update :eta date/from-db)))
;
;
;
;(defn group-allocations [batches]
;  (reduce (fn [result current_batch] (update result :allocations into (:allocations current_batch)))
;          batches)
;  )
;
;
;(defrecord SqlRepository [db]
;  AbstractRepository
;  (save [_ batch]
;    (let [{:keys [allocations]} batch
;          new-batch (jdbc/insert! db :batches (dissoc batch :allocations))
;          new-lines (pmap #(jdbc/insert! db :order_lines %) allocations)
;          allocations (map
;                        (fn [l] {:order_line_id (:id (first l)) :batch_id (:id (first new-batch))})
;                        new-lines)]
;      (jdbc/insert-multi! db :allocations allocations)))

  ;(get-by-ref [this ref]
  ;  (let [db-spec (:db this)
  ;        batches (jdbc/query db-spec ["select b.quantity , b.ref, b.sku ,b.eta,
  ;                                      ol.sku as order_sku, ol.quantity as order_quantity, ol.order_id
  ;                                     from batches  as b
  ;                                     left join allocations a on b.id = a.batch_id
  ;                                     inner join order_lines ol on ol.id = a.order_line_id
  ;                                     where b.ref = ? " ref]
  ;                            {:row-fn        mount-batch
  ;                             :result-set-fn group-allocations}
  ;                            )]
  ;    batches))

  ;(get-all [this]
  ;  (jdbc/query (:db this) ["select b.quantity , b.ref, b.sku ,b.eta,
  ;                                      ol.sku as order_sku, ol.quantity as order_quantity, ol.order_id
  ;                                     from batches  as b
  ;                                     left join allocations a on b.id = a.batch_id
  ;                                     left join order_lines ol on ol.id = a.order_line_id
  ;                                     order by  eta"]
  ;              {:row-fn        mount-batch
  ;               :result-set-fn (fn [x] (->> (group-by :ref x) vals (map group-allocations)))
  ;               }))
  ;)



;(defn new-sql-repo [db]
;  (SqlRepository. db))
;

(defrecord InMemoryRepository [products]
  AbstractRepository
  (save [this product] (swap! products assoc (:sku product) product))
  (get-by-sku [this sku] (get @products sku))
  (get-by-ref [this ref] (some (fn [prod] (when (some #(= (:ref %) ref) (:batches prod)) prod)) (vals @products)))
  )

(defn new-in-memory-repository []
  (->InMemoryRepository (atom {})))
