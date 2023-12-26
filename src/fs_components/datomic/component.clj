(ns fs-components.datomic.component
  (:require [com.stuartsierra.component :as component]
            [datomic.api :as d]
            [fs-components.config.protocol :as config.protocol]
            [fs-components.datomic.protocol :as datomic.protocol]
            [taoensso.timbre :as log]))

(defrecord DatomicClient [config schemata conn]
  component/Lifecycle
  (start [this]
    (if-not conn
      (let [datomic-uri (config.protocol/get! config [:datomic-uri])
            _ (d/create-database datomic-uri)
            datomic_conn (d/connect datomic-uri)
            _ @(d/transact datomic_conn schemata)]
        (do
          (log/info "Established connection to datomic uri " datomic-uri)
          (doseq [schema schemata]
            (log/info "Transacted attribute " schema))
          (assoc this :conn (atom datomic_conn))))
      (do
        (log/warn "Datomic component is started already")
        this)))
  (stop [this]
    (if conn
      (do
        ; From docs, datomic connections don't adhere to an
        ; acquire/use/release pattern! So no need to explicitly
        ; close the connection, just drop the object in memory
        (reset! (:conn this) nil)
        this)
      (do
        (log/warn "Datomic component already stopped")
        this)))

  datomic.protocol/Datomic
  (conn [this] @(:conn this))
  (db [this] (d/db (datomic.protocol/conn this)))
  (query [this {:keys [query rules params]}]
    (log/info "Performing datomic query...")
    (let [db (datomic.protocol/db this)
          arg-list (filter some?
                           (concat [query db rules] params))]
      (apply d/q arg-list)))
  (pull [this {:keys [pattern entity-ref]}]
    (d/pull (datomic.protocol/db this) pattern entity-ref))
  (transact [this {:keys [tx-data]}]
    (log/info "Performing datomic transaction" tx-data)
    @(d/transact (datomic.protocol/conn this) tx-data)))

(defn new-datomic-client [schemata]
  (map->DatomicClient {:schemata schemata}))
