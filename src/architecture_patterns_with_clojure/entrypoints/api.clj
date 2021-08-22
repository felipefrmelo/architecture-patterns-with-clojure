(ns architecture-patterns-with-clojure.entrypoints.api
  (:require [io.pedestal.http.route :as route]
            [io.pedestal.http :as http]
            [io.pedestal.interceptor :as interceptor]
            [io.pedestal.http.body-params :as body-params]
            [io.pedestal.http.content-negotiation :as conneg]
            [io.pedestal.interceptor.error :as error]
            [clojure.data.json :as json]
            [architecture-patterns-with-clojure.adapters.repository :as repository]
            [architecture-patterns-with-clojure.domain.order :as model]
            [architecture-patterns-with-clojure.adapters.database :as database]
            [clojure.string :as str]))

(defn response [status body & {:as headers}]
  {:status status :body body :headers headers})

(def ok (partial response 200))
(def created (partial response 201))
(def out-of-stock (partial response 400))



(defn get-error-msg [ex]
  (str/replace  (ex-message ex) #"clojure.lang.ExceptionInfo in Interceptor  - " "" )
  )
(def service-error-handler
  (error/error-dispatch [ctx ex]
                        ;; Handle `ArithmeticException`s thrown by `::throwing-interceptor`
                        [{:exception-type :java.lang.ArithmeticException :interceptor ::throwing-interceptor}]
                        (assoc ctx :response {:status 500 :body "Exception caughtss!"})

                        [{:exception-type :clojure.lang.ExceptionInfo}]
                        (assoc ctx :response (out-of-stock (get-error-msg ex)))

                        :else (assoc ctx :response {:status 500 :body (.getMessage ex)})))

(def echo
  {:name  ::echo
   :enter #(assoc % :response (ok (:request %)))})

(def supported-types ["text/html" "application/edn" "application/json" "text/plain"])

(def content-neg-intc (conneg/negotiate-content supported-types))

(defn accepted-type
  [context]
  (get-in context [:request :accept :field] "text/plain"))

(defn transform-content
  [body content-type]
  (case content-type
    "text/html" body
    "text/plain" body
    "application/edn" (pr-str body)
    "application/json" (json/write-str body)))

(defn coerce-to
  [response content-type]
  (-> response
      (update :body transform-content content-type)
      (assoc-in [:headers "Content-Type"] content-type)))

(defn allocate-endpoint [{:keys [headers params json-params path-params] :as request}]
  ;(let [sql-repo (repository/new-sql-repo database/db)
  ;      line (model/new-order-line json-params)
  ;      ;new-batch (model/allocate line batches)
  ;      ]

    ;(repository/add sql-repo new-batch)
    ;(created {:batchref (:ref new-batch)})
    ;)
)



(def coerce-body
  {:name ::coerce-body
   :leave
         (fn [context]
           (cond-> context
                   (nil? (get-in context [:response :headers "Content-Type"]))
                   (update-in [:response] coerce-to (accepted-type context))))})


(def routes
  (route/expand-routes
    #{["/allocate" :post [echo coerce-body content-neg-intc service-error-handler (body-params/body-params) allocate-endpoint] :route-name :allocate]
      ["/echo" :get echo]}))

(def service-map
  {::http/routes routes
   ::http/type   :jetty
   ::http/port   5000})

(defn start []
  (http/start (http/create-server service-map)))

;; For interactive development
(defonce server (atom nil))                                 ;; <1>

(defn start-dev []
  (reset! server                                            ;; <2>
          (http/start (http/create-server
                        (assoc service-map
                          ::http/join? false)))))           ;; <3>

(defn stop-dev []
  (http/stop @server))

(defn restart []                                            ;; <4>
  (stop-dev)
  (start-dev))

;(if (nil? @server)
;  (do (println "Server starts") (start-dev))
;  (restart))
;(stop-dev)
;(restart)



