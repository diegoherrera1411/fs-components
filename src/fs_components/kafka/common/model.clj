(ns fs-components.kafka.common.model)

(def consume-topic :kafka.interaction/consume)
(def produce-topic :kafka.interaction/produce)
(def full-topic :kafka.interaction/all)

(defn is-type-config? [topic-type {:keys [type]}]
  (or (= type topic-type)
      (= type full-topic)))

(def is-consumer-config? (partial is-type-config? consume-topic))
(def is-producer-config? (partial is-type-config? produce-topic))

(defn type-topics-from-config [topic-pred kafka-config]
  (->> kafka-config
       (filterv (fn [[_ topic-config]]
                  (topic-pred topic-config)))
       (into (sorted-map))))

(def consumer-topics-from-config (partial type-topics-from-config
                                          is-consumer-config?))
(def producer-topics-from-config (partial type-topics-from-config
                                          is-producer-config?))
