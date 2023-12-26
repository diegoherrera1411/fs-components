(ns fs-components.kafka.producer.component
  (:require [com.stuartsierra.component :as component]
            [fs-components.kafka.producer.client :as producer.client]
            [fs-components.kafka.producer.protocol :as producer.protocol]
            [fs-components.kafka.common.model :as kafka.model]
            [fs-components.config.protocol :as config.protocol]
            [taoensso.timbre :as log]))

(defrecord ProducerClient [config producer-config]
  component/Lifecycle
  (start [this]
    (if-not producer-config
      (let [kafka-config (config.protocol/get! config [:kafka-topics])
            kafka-server (config.protocol/get! config [:kafka-server])
            produce-topics (kafka.model/producer-topics-from-config kafka-config)
            produce-topics-kw (set (keys produce-topics))
            producer-client (producer.client/create! {:server kafka-server})]
        (assoc this
          :producer-config (atom
                             {:producer-obj producer-client
                              :topic-kws    produce-topics-kw
                              :topic-config produce-topics})))
      (do
        (log/warn "Producer component already started")
        this)))
  (stop [this]
    (let [producer-config @(get this :producer-config)
          producer-client (get producer-config :producer-obj)]
      (if producer-config
        (do
          (producer.client/stop! producer-client)
          (reset! producer-config nil))
        (do
          (log/warn "Producer component already stopped")
          this))))

  producer.protocol/Producer
  (topic-kw->topic-str [this topic-kw]
    (let [topic-config (get-in @(:producer-config this) [:topic-config topic-kw])]
      (if (seq topic-config)
        (:topic-str topic-config)
        (throw (Exception. (str "The topic" topic-kw
                                " is not registered in the system configuration"))))))
  (produce! [this payload]
    (let [producer-config (get this :producer-config)
          producer-client (get @producer-config :producer-obj)
          topic-str (producer.protocol/topic-kw->topic-str this (:topic payload))]
      (producer.client/produce! producer-client
                                (assoc payload :topic topic-str)))))

(defn new-producer-client []
  (map->ProducerClient {}))