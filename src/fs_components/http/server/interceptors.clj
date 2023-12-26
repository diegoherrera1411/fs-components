(ns fs-components.http.server.interceptors
  (:require [io.pedestal.http.content-negotiation :as content-negotiation]
            [io.pedestal.http.body-params :as http.body]))

(def supported-content-types ["application/edn"
                              "application/json"
                              "text/plain"])

(defn content-negotiation-interceptor
  ([] (content-negotiation/negotiate-content supported-content-types))
  ([types]
   (content-negotiation/negotiate-content types)))

(defn body-params-interceptor []
  (http.body/body-params))

(defn webapp-interceptor [webapp]
  {:name  ::webapp-interceptor
   :enter (fn [context]
            (update context :request assoc :webapp webapp))})
