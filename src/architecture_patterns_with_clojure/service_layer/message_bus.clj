(ns architecture-patterns-with-clojure.service-layer.message-bus
  ;(:require [architecture-patterns-with-clojure.service-layer.handlers :as handlers])
  ;(:import (architecture_patterns_with_clojure.domain.events
  ;           OutOfStock BatchCreated AllocationRequired))
  )



(defmulti handlers class)



(defn handle [events & dep]
  (let [event (first events)]
    (when event
      (doseq [f (handlers event)] (apply f event dep))
      (recur (rest events) dep))
    )
  )
;(apply (fn [x y] (println x)) 1 [2])
;(handle [1 2 3 (events/->OutOfStock "asa")])
