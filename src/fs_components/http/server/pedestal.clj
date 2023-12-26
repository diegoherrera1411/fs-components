(ns fs-components.http.server.pedestal
  (:require [fs-components.utils.environment :as u.env]
            [io.pedestal.http :as http]
            [cheshire.core :as cc]))

(defn server-map [{:keys [routes type port env] :as params}]
  "Starts a pedestal HTTP server. In local env, starts it in interactive
  development mode"
  (http/create-server
    (cond-> {:env          env
             ::http/routes routes
             ::http/type   type
             ::http/port   port}
            (u.env/local? params) (assoc ::http/join? false))))

(defn json-response
  [status body & {:as headers}]
  {:status  status
   :headers (assoc headers "Content-Type" "application/json")
   :body    (cc/generate-string body)})

(defn response
  [status body & {:as headers}]
  {:status status :body body :headers headers})

(def ok (partial response 200))
(def created (partial response 201))
(def accepted (partial response 202))

(def json-ok (partial json-response 200))
(def json-created (partial json-response 201))
(def json-accepted (partial json-response 202))
