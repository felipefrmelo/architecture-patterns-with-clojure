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
                 ]
  :repl-options {:init-ns architecture-patterns-with-clojure.core})
