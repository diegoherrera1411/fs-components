(ns fs-components.webapp.component
  (:require [com.stuartsierra.component :as component]))

(defrecord Webapp []
  component/Lifecycle
  (start [this] this)
  (stop [this] this))

(defn new-webapp [] (map->Webapp {}))
