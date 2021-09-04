(ns architecture-patterns-with-clojure.random-refs

  (:require [clojure.string :as str])
  (:import (java.util UUID)))

(defn random-suffix []
  (str/join "" (take 6 (.toString (UUID/randomUUID)))))

(defn random-sku [& {:keys [name] :or {name ""}}]
  (str "sku-" name "-" (random-suffix)))


(defn random-batchref [& {:keys [name] :or {name ""}}]
  (str "batch-" name "-" (random-suffix)))

(defn random-orderid [& {:keys [name] :or {name ""}}]
  (str "order-" name "-" (random-suffix)))
