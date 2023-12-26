(ns fs-components.kafka.producer.protocol)

(defprotocol Producer
  "Interface for Kafka producer. The payload is a map with properties
  {:topic   keyword?
   :key     string?
   :message map?}"
  (topic-kw->topic-str [producer topic-kw] "Get the topic string from config if exists")
  (produce! [producer payload] "Produce a kafka message to a topic"))