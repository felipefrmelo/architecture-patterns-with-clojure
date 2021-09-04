(ns architecture-patterns-with-clojure.entrypoints.api
  (:require [io.pedestal.http :as http]
            [io.pedestal.http.body-params :as body-params]
            [ring.util.response :as ring-resp]
            [architecture-patterns-with-clojure.service-layer.handlers :as handlers]
            [architecture-patterns-with-clojure.domain.order :as order]
            [architecture-patterns-with-clojure.domain.commands :as commands]
            [architecture-patterns-with-clojure.adapters.repository :as repository]
            [architecture-patterns-with-clojure.domain.product :as product]
            [architecture-patterns-with-clojure.util.date :as date]
            [io.pedestal.interceptor.error :as error]
            [architecture-patterns-with-clojure.util.json :as json]
            [cheshire.generate])
  (:import (java.time LocalDateTime)))


(extend-protocol cheshire.generate/JSONable
  LocalDateTime
  (to-json [dt gen]
    (cheshire.generate/write-string gen (str dt))))

(def in-memory-repo (repository/new-in-memory-repository))

(defn get-err-msg [ex]
  (->> ex ex-data :exception
       ((juxt ex-message (comp :errors ex-data)))
       (zipmap [:msg :errors])
       json/stringify))

(def service-error-handler
  (error/error-dispatch [ctx ex]
                        [{:exception-type :clojure.lang.ExceptionInfo}]
                        (assoc ctx :response {:status 400 :body (get-err-msg ex)})
                        :else (assoc ctx :response {:status 500 :body "Exception caught!"})))


(defn allocate-endpoint [{:keys [json-params] :as request}]
  (handlers/dispatch! (commands/allocate json-params) :repo in-memory-repo)
  (ring-resp/created (:order_id json-params))

  )

(defn add-batch [{:keys [json-params]}]
  (handlers/dispatch! (commands/create-batch (update json-params :eta date/parse)) :repo in-memory-repo)
  (ring-resp/created "opa")
  )


(defn get-p [{:keys [path-params] :as request}]
  (ring-resp/response (repository/get-by-sku in-memory-repo (:sku path-params)))
  )

(defn hello-page [{:keys [path-params] :as request}]
  (ring-resp/response "Hellabccc")
  )



(def common-interceptors [service-error-handler (body-params/body-params) http/json-body])

(def routes #{["/allocate" :post (conj common-interceptors `allocate-endpoint)]
              ["/batch" :post (conj common-interceptors `add-batch)]
              ["/prod/:sku" :get (conj common-interceptors `get-p)]
              ["/a" :get (conj common-interceptors `hello-page)]
              })



(def service {:env                     :prod
              ::http/routes            routes
              ::http/type              :jetty
              ::http/port              8080
              ::http/host              "0.0.0.0"
              ::http/container-options {:h2c? true
                                        :h2?  false
                                        :ssl? false
                                        }})


