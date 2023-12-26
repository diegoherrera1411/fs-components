(ns fs-components.kafka.producer.client
  (:gen-class)
  (:require [fs-components.kafka.common.serializers :as k.serializers]
            [taoensso.timbre :as log]
            [cheshire.core :as cc])
  (:import (java.util Properties)
           (org.apache.kafka.clients.producer KafkaProducer ProducerConfig ProducerRecord Callback)))

(def config {:server           "localhost:9092"
             :key-serializer   k.serializers/string-serializer
             :value-serializer k.serializers/string-serializer})

(defn- map->producer-properties [{:keys [server key-serializer value-serializer]}]
  (doto (Properties.)
    (.putAll {ProducerConfig/BOOTSTRAP_SERVERS_CONFIG      server
              ProducerConfig/KEY_SERIALIZER_CLASS_CONFIG   key-serializer
              ProducerConfig/VALUE_SERIALIZER_CLASS_CONFIG value-serializer})))

(defn create!
  ([] (create! config))
  ([custom-params]
   (let [custom-config (merge config custom-params)
         ^Properties properties (map->producer-properties custom-config)]
     (KafkaProducer. properties))))

(defn format-metadata [metadata]
  (format "Produced record to topic %s partition [%d] @ offset %d"
          (.topic metadata)
          (.partition metadata)
          (.offset metadata)))

(defn- print-result [_ metadata exception]
  "TODO: when exception, send message to dead letter :)"
  (if exception
    (log/error "Failed to deliver message: " exception)
    (-> metadata (format-metadata) (log/info))))

(defn produce! [producer {:keys [topic key value callback]
                          :or   {callback print-result}}]
  (let [str-value (cc/generate-string value)
        msg-record (ProducerRecord. topic key str-value)
        callback-obj (reify Callback (onCompletion [this metadata exception]
                                       (callback this metadata exception)))]
    (try
      @(.send producer msg-record callback-obj)
      (catch Exception e
        (println "Unable to send message. Error: " e)))))

(defn stop! [producer]
  (.flush producer)
  (.close producer)
  producer)
