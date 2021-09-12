(ns architecture-patterns-with-clojure.integration.rabbitmq-test
(:require [langohr.core      :as core] 
            [langohr.channel   :as channel] 
            [langohr.queue     :as queue] 
            [langohr.consumers :as consumers] 
            [langohr.basic     :as basic] 
            [langohr.exchange  :as exchange]             
  ))

(def ^{:const true}
  default-exchange-name "")



(defn simple-producer [qname payload] 
  (with-open 
    [conn (core/connect ) 
     ch (channel/open conn)] 
    (println "producing message to exchange " qname " payload = " payload) 
    (queue/declare ch qname {:exclusive false :auto-delete false :durable true}) 
    (basic/publish 
     ch "" qname payload {:content-type "text/plain" :type "message" :persistent true})) )

(defn message-handler 
  [ch {:keys [content-type delivery-tag type] :as meta} ^bytes payload]
  (when (= (String. payload "UTF-8") "ack") (basic/ack ch delivery-tag))
  (println 
   (str "Received a message payload : " (String. payload "UTF-8")  " , tag : "  delivery-tag))) 


(defn blocking-consumer [qname] 
  (let [conn (core/connect ) 
        ch (channel/open conn)] 
    (println (str "simple-consumer is connected with id: "(.getChannelNumber ch)))
    (queue/declare ch qname {:exclusive false :auto-delete false :durable true}) 
    (consumers/subscribe ch qname message-handler {:auto-ack false})
    #(run! (fn [c] (when-not (core/closed? c) (core/close c))) [conn ch]))) 



;; (simple-producer "felipe" "no-ack")

;; (def c1 (blocking-consumer "felipe"))

;; (c1)

;; (def c2 (blocking-consumer "felipe"))

;; (c2)
