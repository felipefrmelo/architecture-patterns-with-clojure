(ns architecture-patterns-with-clojure.entrypoints.event-consumer
  (:gen-class)
  (:require [architecture-patterns-with-clojure.adapters.repository :as repository]
            [architecture-patterns-with-clojure.adapters.event-publisher :as event-publisher]
            [architecture-patterns-with-clojure.domain.commands :as commands]
            [architecture-patterns-with-clojure.service-layer.handlers :as handlers]
            [architecture-patterns-with-clojure.util.json :as json]
            [langohr.channel :as lch]
            [langohr.consumers :as lc]
            [langohr.core :as rmq]
            [langohr.queue :as lq]))

(def repo (repository/new-mongo-repository))
(def pubsub (event-publisher/new-pubsub-rmq))
(def send-email identity)

(defn bytes-to-edn [payload]
  (-> payload
      (String. "UTF-8")
      (json/parse)))



(defn handle-change-batch-quantity [_ _ msg]
  (let [dispatch! (handlers/make-dispatch repo pubsub send-email identity)
        cmd (commands/command :change-batch-quantity msg)]
    (dispatch! cmd)))



(defn start []
  (let [conn  (event-publisher/consumer pubsub event-publisher/qname handle-change-batch-quantity)
        ]

    conn
   ))

(defn -main [& args]
  (let [conn (start)]
    (.addShutdownHook (Runtime/getRuntime) (Thread. #(rmq/close conn))))
  )


