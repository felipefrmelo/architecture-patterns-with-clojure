(ns architecture-patterns-with-clojure.fixtures
  (:require 
   [architecture-patterns-with-clojure.adapters.database :as database]
   [monger.core :as mg]
   [clojure.java.jdbc :as jdbc]))


(defn clean-db [f]
  (f)
  (let [conn (mg/connect)]
    (mg/drop-db conn "monger-test")
    (mg/disconnect conn))  )

