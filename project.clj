(defproject architecture-patterns-with-clojure "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url  "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.10.1"]
                 [nubank/matcher-combinators "1.5.1"]
                 [midje "1.9.9"]
                 [org.clojure/java.jdbc "0.7.12"]
                 [com.h2database/h2 "1.4.200"]
                 [com.mchange/c3p0 "0.9.5.2"]
                 [org.postgresql/postgresql "42.1.4"]
                 [clj-http "3.12.3"]
                 [io.pedestal/pedestal.service "0.5.7"]
                 [io.pedestal/pedestal.route "0.5.7"]
                 [io.pedestal/pedestal.jetty "0.5.7"]
                 [org.slf4j/slf4j-simple "1.7.28"]
                 [org.clojure/data.json "0.2.6"]
                 [cheshire "5.10.0"]
                 ]
  :repl-options {:init-ns architecture-patterns-with-clojure.core}

  :min-lein-version "2.0.0"
  :resource-paths ["config", "resources"]
  ;; If you use HTTP/2 or ALPN, use the java-agent to pull in the correct alpn-boot dependency
  ;:java-agents [[org.mortbay.jetty.alpn/jetty-alpn-agent "2.0.5"]]
  :profiles {:dev {:plugins [[lein-midje "3.2.1"]]
                   :aliases {"run-dev" ["trampoline" "run" "-m" "architecture-patterns-with-clojure.entrypoints.server/run-dev"]}}
             :uberjar {:aot [architecture-patterns-with-clojure.entrypoints.server]}}
  :main ^{:skip-aot true} architecture-patterns-with-clojure.entrypoints.server
  )
