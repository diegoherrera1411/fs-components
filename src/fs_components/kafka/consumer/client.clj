(ns fs-components.kafka.consumer.client
  (:gen-class)
  (:require [cheshire.core :as cc]
            [fs-components.utils.names :as u.names]
            [fs-components.kafka.common.serializers :as k.serializers]
            [taoensso.timbre :as log])
  (:import (java.util Properties)
           (org.apache.kafka.clients.consumer ConsumerRecord ConsumerRecords KafkaConsumer ConsumerConfig)
           (org.apache.kafka.common.errors WakeupException)))

(def base-config
  {:server             "localhost:9092"
   :group-id           "1"
   :key-deserializer   k.serializers/string-deserializer
   :value-deserializer k.serializers/string-deserializer})

(defn- map->consumer-properties [{:keys [server group-id
                                         key-deserializer
                                         value-deserializer]}]
  (doto (Properties.)
    (.putAll {ConsumerConfig/GROUP_ID_CONFIG                 group-id
              ConsumerConfig/BOOTSTRAP_SERVERS_CONFIG        server
              ConsumerConfig/KEY_DESERIALIZER_CLASS_CONFIG   key-deserializer
              ConsumerConfig/VALUE_DESERIALIZER_CLASS_CONFIG value-deserializer})))

(defn create!
  ([] (create! base-config))
  ([custom-params]
   (let [custom-config (merge base-config custom-params)
         ^Properties properties (map->consumer-properties custom-config)]
     (KafkaConsumer. properties))))

(defn consumer-record->map [^ConsumerRecord record]
  {:key                   (.key record)
   :offset                (.offset record)
   :partition             (.partition record)
   :serialized-key-size   (.serializedKeySize record)
   :serialized-value-size (.serializedValueSize record)
   :timestamp             (.timestamp record)
   :timestamp-type        (.timestampType record)
   :topic                 (.topic record)
   :value                 (.value record)
   :consumer-record       record})

(defn handle-consumer-error [topic e]
  (log/error "Unable to consume kafka message: "
             {:topic      topic
              :msg        (.getMessage e)
              :error-type (-> e (.getClass) (.getCanonicalName))}))

(defn dispatch-record [record handler]
  (let [{:keys [topic value key offset partition]} (consumer-record->map record)]
    (log/info "Consuming message: "
              {:topic     topic
               :offset    offset
               :partition partition
               :value     value
               :key       key})
    (handler (cc/parse-string value true))))

(defn poll-and-dispatch [consumer handler]
  (let [^ConsumerRecords messages (.poll consumer (long 100))]
    (doseq [record (iterator-seq (.iterator messages))]
      (dispatch-record record handler))))

(defn start! [consumer topic handler]
  (log/debug "Starting consumer: "
             {:topic   topic
              :handler (u.names/fn-name handler)})
  (let [started? (atom true)
        _ (.subscribe consumer [topic])
        process (future
                  (while @started?
                    (try
                      (poll-and-dispatch consumer handler)
                      (catch Exception e
                        (handle-consumer-error topic e)))))]
    {:started?  started?
     :topic-str topic
     :consumer  consumer
     :process   process}))

(defn stop! [{:keys [consumer started? process] :as state}]
  (try
    (reset! started? false)
    (.wakeup consumer)
    (deref process 100 :timeout)
    state
    (catch WakeupException we
      (log/info "Stopped consumer: " (.getMessage we)))))
