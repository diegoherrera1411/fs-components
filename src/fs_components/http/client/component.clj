(ns fs-components.http.client.component
  (:require [cheshire.core :as cc]
            [clj-http.conn-mgr :as conn]
            [clj-http.core :as core]
            [com.stuartsierra.component :as component]
            [fs-components.config.protocol :as config.protocol]
            [fs-components.http.client.protocol :as http-client.protocol]
            [fs-components.utils.names :as u.names]
            [clj-http.client :as client]
            [taoensso.timbre :as log]))

(defn full-request-url [config {:keys [url] :as http-op} {:keys [path-params]}]
  (let [{base-url-kw :base-url} http-op
        base-url (config.protocol/get! config [:http-client :services base-url-kw])]
    (u.names/http-op-url base-url url path-params)))

(defn default-params [params]
  (cond-> params
          (nil? (:as params)) (assoc :as :json)
          (= (:content-type params) :json) (update :body cc/generate-string)))

(defn http-clients-from-server-config [http-servers conn-manager]
  (reduce
    (fn [conn-list [server-name _]]
      (assoc conn-list server-name (core/build-http-client {} false conn-manager)))
    {}
    http-servers))

(defrecord HttpClient [config conn]
  component/Lifecycle
  (start [this]
    (if-not (and conn @conn)
      (let [http-servers (config.protocol/get! config [:http-client :services])
            conn-manager (conn/make-reusable-conn-manager {})
            http-clients (http-clients-from-server-config http-servers conn-manager)]
        (assoc this :conn (atom {:conn-manager conn-manager
                                 :http-clients http-clients})))
      (log/warn "Http Client component already started")))
  (stop [this]
    (if (and conn @conn)
      (let [conn (:conn this)]
        (do
          (conn/shutdown-manager conn)
          (reset! conn nil)))
      (log/warn "Http Client component already stopped")))

  http-client.protocol/HttpClient
  (conn-manager [this] (:conn-manager @(:conn this)))
  (http-client [this http-client-kw] (get-in @(:conn this) [:http-clients http-client-kw]))
  (req! [this http-op-kw params]
    (let [http-op (config.protocol/get! config [:http-client :endpoints http-op-kw])
          req-url (full-request-url config http-op params)
          conn (http-client.protocol/conn-manager this)
          http-client (http-client.protocol/http-client this http-op-kw)
          params* (default-params params)]
      (try
        (do
          (if (empty? req-url) (throw (Exception. "Request URL doesn't exist")))
          (log/info (str "Request to " req-url) params*)
          (client/request (merge
                            params*
                            {:method             (:method http-op)
                             :url                req-url
                             :connection-manager conn
                             :http-client        http-client})))
        (catch Exception e
          (log/error (.getMessage e)))))))

(defn new-http-client []
  (map->HttpClient {}))
