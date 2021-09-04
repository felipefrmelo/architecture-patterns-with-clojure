(ns architecture-patterns-with-clojure.service-layer.message-bus
  ;(:require [architecture-patterns-with-clojure.service-layer.handlers :as handlers])
  ;(:import (architecture_patterns_with_clojure.domain.events
  ;           OutOfStock BatchCreated AllocationRequired))
  (:require [architecture-patterns-with-clojure.domain.events :as events]
            [clojure.core.async :refer [put! go-loop <! sub chan pub]]))

(ns-unmap *ns* 'event-handlers)

(defonce event-ch (chan))
(defonce event-bus (pub event-ch ::type))



(defn emit!
  ([event]
   (put! event-ch {::type    (:type event)
                   ::payload (:payload event)})))

(defn subscribe! [topic handle]
  (let [added (chan)]
    (sub event-bus topic added true)
    (go-loop []
      (handle (::payload (<! added)))
      (recur)
      )))










