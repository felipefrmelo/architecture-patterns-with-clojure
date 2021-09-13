(ns architecture-patterns-with-clojure.entrypoints.server
  (:gen-class) ; for -main method in uberjar
  (:require [io.pedestal.http :as server]
            [io.pedestal.http.route :as route]
            [architecture-patterns-with-clojure.entrypoints.api :as api]
            [architecture-patterns-with-clojure.adapters.repository :as repository]
            [architecture-patterns-with-clojure.adapters.event-publisher :as event-publisher]
            [architecture-patterns-with-clojure.service-layer.handlers :as handlers]
            [architecture-patterns-with-clojure.views :as views]))

(def repo (repository/new-mongo-repository))
(def send-email identity)

(defonce runnable-service 
  (server/create-server 
   (api/service (handlers/make-dispatch repo (event-publisher/new-pubsub-rmq) send-email views/insert-view) )))



(defn run-dev
  "The entry-point for 'lein run-dev'"
  [& args]
  (println "\nCreating your [DEV] server...")
  (-> (api/service repo) ;; start with production configuration
      (merge {:env :dev
              ;; do not block thread that starts web server
              ::server/join? false
              ;; Routes can be a function that resolve routes,
              ;;  we can use this to set the routes to be reloadable
              ::server/routes #(route/expand-routes (deref (api/routes repo)))
              ;; all origins are allowed in dev mode
              ::server/allowed-origins {:creds true :allowed-origins (constantly true)}
              ;; Content Security Policy (CSP) is mostly turned off in dev mode
              ::server/secure-headers {:content-security-policy-settings {:object-src "'none'"}}})
      ;; Wire up interceptor chains
      server/default-interceptors
      server/dev-interceptors
      server/create-server
      server/start))
  

(defn -main
  "The entry-point for 'lein run'"
  [& args]
  (println "\nCreating your server...")
  (server/start runnable-service))

;(def serv (run-dev))
;(server/stop serv)

