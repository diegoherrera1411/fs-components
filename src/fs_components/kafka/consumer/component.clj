(ns fs-components.kafka.consumer.component
  (:require [com.stuartsierra.component :as component]
            [fs-components.kafka.consumer.client :as consumer.client]
            [fs-components.config.protocol :as config.protocol]
            [fs-components.kafka.common.model :as kafka.model]
            [fs-components.utils.names :as u.names]
            [taoensso.timbre :as log]))

(defn consumer-adapted-handler [webapp handler]
  (partial handler webapp))

(defn start-consumer! [kafka-server webapp {:keys [topic-str handler]}]
  (let [consumer (consumer.client/create! {:group-id (u.names/fn-name handler)
                                           :server   kafka-server})]
    (consumer.client/start! consumer topic-str (consumer-adapted-handler webapp handler))))

(defn start-consumers!
  [kafka-server webapp consumer-topics topics-config]
  (reduce-kv
    (fn [consumers-state-map topic-kw {:keys [handler partition-number]}]
      (let [topic-str (get-in consumer-topics [topic-kw :topic-str])]
        (if topic-str
          (assoc-in consumers-state-map
                    [topic-kw :consumer-instances] (mapv
                                                     (fn [_]
                                                       (start-consumer! kafka-server webapp {:topic-str topic-str
                                                                                             :handler   handler}))
                                                     (range partition-number)))
          (throw (Exception. "Consumer topic not found.")))))
    {}
    topics-config))

(defn stop-consumers! [consumers-state]
  (reduce-kv
    (fn [closed-state topic-kw current-state]
      (let [closed-topic-state (update current-state
                                       :consumer-instances
                                       #(mapv consumer.client/stop! %))]
        (assoc closed-state topic-kw closed-topic-state)))
    {}
    consumers-state))

(defrecord ConsumerClient [config webapp topic-config consumers-state]
  component/Lifecycle
  (start [this]
    (if-not consumers-state
      (let [kafka-config (config.protocol/get! config [:kafka-topics])
            kafka-server (config.protocol/get! config [:kafka-server])
            consumer-topics (kafka.model/consumer-topics-from-config kafka-config)
            consumer-states (start-consumers! kafka-server webapp consumer-topics topic-config)]
        (assoc this
          :consumers-state (atom (merge-with merge consumer-states consumer-topics))))
      (do
        (log/warn "Consumer component already started")
        this)))
  (stop [this]
    (let [consumers-state (:consumers-state this)]
      (if @consumers-state
        (do
          (stop-consumers! @consumers-state)
          (reset! consumers-state nil)
          (assoc this :topic-config nil))
        (do
          (log/warn "Consumer component already stopped")
          this)))))

(defn new-consumer-client [topic-config]
  (map->ConsumerClient {:topic-config topic-config}))
