(ns versiontracker.db.core
  (:require
    [next.jdbc.date-time]
    [next.jdbc.result-set]
    [conman.core :as conman]
    [mount.core :refer [defstate]]
    [versiontracker.config :refer [env]]))

(defstate ^:dynamic *db*
  "This variable has the database connection to the database"
  :start
   (let [jdbc-h2    (:h2 (env :database-url))
         jdbc-mssql (:mssql (env :database-url))]
     (if (nil? jdbc-mssql)
       (conman/connect! {:jdbc-url jdbc-h2})
       (conman/connect! {:jdbc-url jdbc-mssql})))
  :stop
   (conman/disconnect! *db*))

; How can I make this line dependent of the database
; Different set of queries per database type
(conman/bind-connection *db* "sql/queries.sql")

(extend-protocol next.jdbc.result-set/ReadableColumn
  java.sql.Timestamp
  (read-column-by-label [^java.sql.Timestamp v _]
    (.toLocalDateTime v))
  (read-column-by-index [^java.sql.Timestamp v _2 _3]
    (.toLocalDateTime v))
  java.sql.Date
  (read-column-by-label [^java.sql.Date v _]
    (.toLocalDate v))
  (read-column-by-index [^java.sql.Date v _2 _3]
    (.toLocalDate v))
  java.sql.Time
  (read-column-by-label [^java.sql.Time v _]
    (.toLocalTime v))
  (read-column-by-index [^java.sql.Time v _2 _3]
    (.toLocalTime v)))
