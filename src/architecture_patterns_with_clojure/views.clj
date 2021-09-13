(ns architecture-patterns-with-clojure.views
  (:require [monger.collection :as mc]
            [monger.core :as mg]
            ))



(defn insert-view [doc]
  (mc/insert (mg/get-db (mg/connect) "monger-test") "allocations_views" doc)
  )



(defn allocations [order_id ]
  (println order_id)
  (let [result (mc/find-maps
                                        ;   (:db (repository/new-mongo-repository))
           (mg/get-db (mg/connect) "monger-test")
           "allocations_views"
           {:order_id order_id}
           )]
    (map #(dissoc % :_id) result))

  )

