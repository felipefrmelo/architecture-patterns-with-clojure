(ns architecture-patterns-with-clojure.fixtures
  (:require 
            [architecture-patterns-with-clojure.adapters.database :as database]
            [clojure.java.jdbc :as jdbc]))

(defn db-fixture [f]
  (database/load-tables)
  (f)
  (database/drop-tables)
  )




(defn add-stock [db lines]
  (jdbc/insert-multi! db :batches lines)
  )

