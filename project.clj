(defproject architecture-patterns-with-clojure "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url  "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[cheshire "5.10.0"]
                 [clj-http "3.12.3"]
                 [com.h2database/h2 "1.4.200"]
                 [com.mchange/c3p0 "0.9.5.2"]
                 [com.novemberain/langohr "5.1.0"]
                 [com.novemberain/monger "3.1.0"]
                 [com.taoensso/carmine "3.1.0"]
                 [io.pedestal/pedestal.jetty "0.5.7"]
                 [io.pedestal/pedestal.route "0.5.7"]
                 [io.pedestal/pedestal.service "0.5.7"]
                 [midje "1.9.9"]
                 [nubank/matcher-combinators "1.5.1"]
                 [org.clojure/clojure "1.10.1"]
                 [org.clojure/data.json "0.2.6"]
                 [org.clojure/java.jdbc "0.7.12"]
                 [org.postgresql/postgresql "42.1.4"]
                 [org.slf4j/slf4j-simple "1.7.28"]
                 [com.stuartsierra/component "1.0.0"]]
  :repl-options {:init-ns architecture-patterns-with-clojure.core}

  :min-lein-version "2.0.0"
  :resource-paths ["config", "resources"]
  ;; If you use HTTP/2 or ALPN, use the java-agent to pull in the correct alpn-boot dependency
                                        ;:java-agents [[org.mortbay.jetty.alpn/jetty-alpn-agent "2.0.5"]]

  :profiles {:api {:main architecture-patterns-with-clojure.entrypoints.server
                   :uberjar-name "api.jar"
                   :target-path "target/%s"}
             :pubsub {:main architecture-patterns-with-clojure.entrypoints.event-consumer
                      :uberjar-name "event-consumer.jar"
                      :target-path "target/%s"}
             :dev {:plugins [[lein-midje "3.2.1"]]
                   :aliases {"run-dev" ["trampoline" "run" "-m" "architecture-patterns-with-clojure.entrypoints.server/run-dev"]}}
             
             :uberjar {:aot :all}}


  )
