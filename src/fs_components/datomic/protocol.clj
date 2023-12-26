(ns fs-components.datomic.protocol)

(defprotocol Datomic
  "Interface for interacting with a Datomic component"
  (conn [datomic] "Gets the current connection")
  (db [datomic] "Gets the database snapshot for the current connection")
  (query [datomic args] "Performs a query to the db")
  (pull [datomic args] "Interface for the Datomic pull API")
  (transact [datomic args] "Performs a transaction to the Datomic DB"))
