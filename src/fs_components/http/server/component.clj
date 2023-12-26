(ns fs-components.http.server.component
  (:require [com.stuartsierra.component :as component]
            [fs-components.config.protocol :as config.protocol]
            [fs-components.utils.environment :as u.env]
            [fs-components.http.server.pedestal :as http.pedestal]
            [fs-components.http.server.interceptors :as interceptors]
            [io.pedestal.http :as http]
            [io.pedestal.http.route :as route]
            [taoensso.timbre :as log]))

(defn webapp-enabled-route [webapp route-vector]
  (let [default-interceptors [(interceptors/body-params-interceptor)
                              (interceptors/webapp-interceptor webapp)]]
    (if (vector? (get route-vector 2))
      (update route-vector 2 #(concat %2 %1) default-interceptors)
      (update route-vector 2 #(conj %2 %1) default-interceptors))))

(defn webapp-enabled-routes [webapp routes]
  (->> routes
       (mapv (partial webapp-enabled-route webapp))
       (set)))

(defrecord Http [config webapp routes service]
  component/Lifecycle
  (start [this]
    (let [env (config.protocol/get! config [:env])
          http-port (config.protocol/get! config [:http-port])
          http-type (config.protocol/get! config [:http-type])
          enabled-routes (webapp-enabled-routes webapp routes)
          map-with-env (http.pedestal/server-map {:routes (route/expand-routes enabled-routes)
                                                  :port   http-port
                                                  :type   http-type
                                                  :env    env})]
      (if-not service
        (assoc this
          :routes enabled-routes
          :service (cond-> (http/server map-with-env)
                           (u.env/not-test? map-with-env) (http/start)))
        (do
          (log/warn "HTTP Server component already started")
          this))))
  (stop [this]
    (if service
      (do
        (when (u.env/not-test? service) (http/stop service))
        (assoc this
          :routes nil
          :service nil))
      (do
        (log/warn "HTTP Server component already stopped")
        this))))

(defn new-http-server [routes]
  (map->Http {:routes routes}))
