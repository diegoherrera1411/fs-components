(ns fs-components.config.component
  (:require [com.stuartsierra.component :as component]
            [fs-components.utils.files :as u.files]
            [fs-components.config.protocol :as p.config]
            [taoensso.timbre :as log]))

(defrecord ConfigClient [project-name env config-filepath config]
  component/Lifecycle
  (start [this]
    (if-not config
      (let [config-filepath (u.files/config-filepath project-name env)
            config-map (u.files/parse-edn-file config-filepath)]
        (assoc this
          :config (atom (assoc config-map
                          :project-name project-name
                          :env env
                          :config-filepath config-filepath))))
      (do
        (log/warn "Config component already started")
        this)))
  (stop [this]
    (let [config (:config this)
          started-config? (and config (seq @config))]
      (if started-config?
        (do
          (reset! config nil)
          (assoc this
            :project-name nil
            :config-filepath nil
            :env nil))
        (do
          (log/warn "Config component already stopped")
          this))))

  p.config/Config
  (get! [this path]
    (get-in @(:config this) path)))

(defn new-config-client
  ([project-name] (new-config-client project-name :dev))
  ([project-name env]
   (map->ConfigClient {:project-name project-name
                       :env          env})))
