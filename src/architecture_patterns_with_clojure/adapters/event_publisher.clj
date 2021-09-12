(ns architecture-patterns-with-clojure.adapters.event-publisher
  (:require
   [architecture-patterns-with-clojure.util.json :as json]
   [langohr.basic :as lb]
   [langohr.channel :as lch]
   [langohr.consumers :as lc]
   [langohr.core :as rmq]
   [langohr.queue :as lq]))

(defprotocol PubSub
  (producer [this qname payload])
  (consumer [this qname handle]))


(def ^{:const true} default-exchange-name "")

(def qname "change_batch_quantity")

(defn- bytes-to-edn [payload]
  (-> payload
      (String. "UTF-8")
      (json/parse)))

(defn- convert [f]
  (fn [ch meta ^bytes payload]
    (f ch meta (bytes-to-edn payload))))


(defrecord Rbq []
    PubSub
    (producer [_ qname payload] 
      (with-open 
        [conn (rmq/connect ) 
         ch (lch/open conn)] 
        (println "producing message to exchange " qname " payload = " payload) 
        (lq/declare ch qname {:exclusive false :auto-delete false}) 
        (lb/publish 
         ch default-exchange-name qname
         (json/stringify payload) {:content-type "application/json" :type "message" :persistent true})) )
    
    (consumer [_ qname handle] 
      (let [conn (rmq/connect ) 
            ch (lch/open conn)] 
        (println (str "simple-consumer is connected with " qname " event"))
        (lq/declare ch qname {:exclusive false :auto-delete false}) 
        (lc/subscribe ch qname (convert handle) {:auto-ack true})
        conn)))

(defn new-pubsub-rmq []
  (->Rbq ))


(defrecord Dummy []
  PubSub
  (producer [this arg1 arg2] )
  (consumer [this arg1 arg2] )
  
  )

(defn new-pubsub-dummy []
  (->Dummy))
