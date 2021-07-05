(ns architecture-patterns-with-clojure.fixtures
  (:require [clojure.test :refer :all]
            [architecture-patterns-with-clojure.database :as database]
            [clojure.java.jdbc :as jdbc]))

(defn db-fixture [f]
  (database/load-tables)
  (f)
  (database/drop-tables)
  )

