(ns architecture-patterns-with-clojure.database
  (:require [clojure.java.jdbc :as jdbc])
  (:import (com.mchange.v2.c3p0 ComboPooledDataSource)))

(def db-spec
  {
   :classname   "org.postgresql.Driver"
   :subprotocol "postgresql"
   :subname     "//localhost:5432/postgres"
   :user        "postgres"
   :password    "password"})

(defn pool
  [spec]
  (let [cpds (doto (ComboPooledDataSource.)
               (.setDriverClass (:classname spec))
               (.setJdbcUrl (str "jdbc:" (:subprotocol spec) ":" (:subname spec)))
               (.setUser (:user spec))
               (.setPassword (:password spec))
               (.setMaxIdleTimeExcessConnections (* 15 60))
               )
        ]
    {:datasource cpds}))

(def pooled-db (delay (pool db-spec)))


(def db @pooled-db)

(def ^:private create-batches-ddl "create table if not exists batches
(
          id  serial not null constraint
              batches_pk primary key,
          ref       varchar(255),
          sku       varchar(255),
          quantity  integer   not null,
          eta       timestamp not null
            )")


(def ^:private create-order-lines-ddl "create table if not exists order_lines
  (
        id serial not null
         constraint order_lines_pk
        primary key,
        sku varchar(255),
        quantity integer not null,
        order_id varchar(255)
       )")

(def ^:private create-allocations-ddl "CREATE TABLE if not exists allocations (
    id  serial not null constraint allocations_pk   primary key,
    order_line_id integer constraint allocations_order_lines_id_fk  references order_lines
      on update cascade on delete cascade,
    batch_id integer constraint allocations_batches_id_fk references batches
      on update cascade on delete cascade)")


(defn load-tables
  []
  (jdbc/db-do-commands db [create-batches-ddl create-order-lines-ddl create-allocations-ddl]))

(defn drop-tables []
  (jdbc/db-do-commands db ["drop table allocations cascade"
                           "drop table batches cascade "
                           "drop table order_lines cascade"]))

