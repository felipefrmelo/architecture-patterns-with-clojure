(ns architecture-patterns-with-clojure.entrypoints.api
  (:require [architecture-patterns-with-clojure.domain.commands :as commands]
            [architecture-patterns-with-clojure.util.date :as date]
            [architecture-patterns-with-clojure.util.json :as json]
            [architecture-patterns-with-clojure.views :as views]
            cheshire.generate
            [io.pedestal.http :as http]
            [io.pedestal.http.body-params :as body-params]
            [io.pedestal.interceptor.error :as error]
            [ring.util.response :as ring-resp])
  (:import java.time.LocalDateTime))

(extend-protocol cheshire.generate/JSONable
  LocalDateTime
  (to-json [dt gen]
    (cheshire.generate/write-string gen (str dt))))



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


(defn allocate-endpoint [{:keys [json-params  dispatch!] :as request}] 
 (dispatch! (commands/allocate json-params))
  (ring-resp/created (:order_id json-params)))


(defn add-batch [{:keys [json-params dispatch!]}]
  (dispatch! (commands/create-batch (update json-params :eta date/parse)))
  (ring-resp/created "opa"))

(defn allocations_views_endpoint [{:keys [path-params]}]
  (let [order_id (:order_id path-params)
        result (views/allocations order_id)]
  (ring-resp/response result)
    ;; (if result
    ;;   (ring-resp/response result)
    ;;   (ring-resp/not-found "Not found")
    
    ;;   )
    ))

;; @app.route("/allocations/<orderid>", methods=["GET"])
;; def allocations_view_endpoint(orderid):
;;     uow = unit_of_work.SqlAlchemyUnitOfWork()
;;     result = views.allocations(orderid, uow)
;;     if not result:
;;         return "not found", 404
;;     return jsonify(result), 200


(def common-interceptors [service-error-handler (body-params/body-params) http/json-body])

(defn- make-db-interceptor [dispatch]
  {:name :database-interceptor
   :enter
   (fn [context]
     (update context :request assoc :dispatch! dispatch))
   })


(defn routes [dispatch]
  (let [db-interceptor (make-db-interceptor dispatch)]
    #{["/allocate" :post (conj common-interceptors db-interceptor  `allocate-endpoint)]
      ["/batch" :post (conj common-interceptors db-interceptor `add-batch)]
      ["/allocations/:order_id" :get (conj common-interceptors `allocations_views_endpoint)]
      }))


(defn service
  ([dispatch] {:env                     :prod
         ::http/routes            (routes dispatch)
         ::http/type              :jetty
         ::http/port              8080
         ::http/host              "0.0.0.0"
         ::http/container-options {:h2c? true
                                   :h2?  false
                                   :ssl? false
                                   }}))




