(ns architecture-patterns-with-clojure.util.json
  (:require [clojure.data.json :as json]))

(defn stringify [string]
  (json/write-str string))

(defn parse [string]
  (json/read-str string :key-fn keyword))

