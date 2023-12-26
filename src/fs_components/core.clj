(ns fs-components.core
  (:require [com.stuartsierra.component :as component]
            [fs-components.config.component :as config.component]
            [fs-components.datomic.component :as datomic.component]
            [fs-components.datomic.protocol :as datomic.protocol]
            [fs-components.http.client.protocol :as http-client.protocol]
            [fs-components.kafka.consumer.component :as consumer.component]
            [fs-components.kafka.producer.component :as producer.component]
            [fs-components.kafka.producer.protocol :as producer.protocol]
            [fs-components.utils.uuid :refer [new-uuid]]
            [fs-components.webapp.component :as webapp.component]
            [fs-components.http.server.component :as http.component]
            [fs-components.http.client.component :as http-client.component]
            [fs-components.http.server.pedestal :as http.pedestal]
            [taoensso.timbre :as log]))

(def test-datomic-schema [{:db/ident       :order/id
                           :db/valueType   :db.type/uuid
                           :db/cardinality :db.cardinality/one
                           :db/unique      :db.unique/identity}
                          {:db/ident       :order/customer-name
                           :db/valueType   :db.type/string
                           :db/cardinality :db.cardinality/one}])

(def test-consumer-config {:new-order
                           {:partition-number 3
                            :handler          (fn [{:keys [producer datomic]} {:keys [customer-name] :as msg}]
                                                (let [order-id (new-uuid)]
                                                  (datomic.protocol/transact
                                                    datomic
                                                    {:tx-data [{:order/id            order-id
                                                                :order/customer-name customer-name}]})
                                                  (producer.protocol/produce!
                                                    producer
                                                    {:topic :validate-fraud
                                                     :key   customer-name
                                                     :value msg})))}
                           :validate-fraud
                           {:partition-number 3
                            :handler          (fn [{:keys [datomic]} {:keys [customer-name]}]
                                                (let [customer-orders (flatten
                                                                        (datomic.protocol/query
                                                                          datomic
                                                                          {:query  '[:find (pull ?order pattern)
                                                                                     :in $ ?customer pattern
                                                                                     :where
                                                                                     [?order :order/customer-name ?customer]]
                                                                           :params [customer-name '[*]]}))]
                                                  (println customer-name "order count:" (count customer-orders))
                                                  (doseq [order customer-orders]
                                                    (println "Found order with id: " (:order/id order)))
                                                  (log/info "FRAUD DETECTOR CALLED FOR CUSTOMER" customer-name)))}})

(defn respond-hello [{:keys [json-params path-params webapp] :as context}]
  (http.pedestal/json-created
    {:response "Created new order! Yay!"
     :body     json-params}
    "Location" "user/id"))

(def test-routes
  #{["/:user-id/order" :post respond-hello :route-name :order]})

(defn system [project-name env]
  (component/system-map
    :config (config.component/new-config-client project-name env)
    :datomic (component/using (datomic.component/new-datomic-client test-datomic-schema) [:config])
    :producer (component/using (producer.component/new-producer-client) [:config])
    :http-client (component/using (http-client.component/new-http-client) [:config])
    :consumer (component/using (consumer.component/new-consumer-client test-consumer-config) [:config :webapp])
    :http (component/using (http.component/new-http-server test-routes) [:config :webapp])
    :webapp (component/using (webapp.component/new-webapp) [:config :producer :datomic :http-client])))

(def new-dev-system (component/start-system (system "fs-components" :local)))

(def http-client (:http-client new-dev-system))
(def producer (:producer new-dev-system))

(comment
  (producer.protocol/produce! producer {:topic :new-order
                                        :key   "SOME-RANDOM-KEY"
                                        :value {:customer-name "DIEGO HERRERA"}})
  (http-client.protocol/req! http-client :create-resource {:body         {:userId 12
                                                                          :body   "my task"
                                                                          :title  "my task title"}
                                                           :content-type :json
                                                           :as           :json})
  (component/stop-system new-dev-system))