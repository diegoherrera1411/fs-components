(defproject org.clojars.alejozen98/fs-components "0.1.0-SNAPSHOT"
  :description "Basic component library for microservices with kafka + datomic + pedestal"
  :url "https://github.com/diegoherrera1411/fs-components"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url  "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.11.1"]
                 [com.datomic/peer "1.0.7021"]
                 [cheshire "5.12.0"]
                 [com.stuartsierra/component "1.1.0"]
                 [com.taoensso/timbre "6.3.1"]
                 [org.apache.kafka/kafka-clients "3.6.0"]
                 [prismatic/schema "1.4.1"]
                 [org.slf4j/slf4j-simple "2.0.9"]
                 [io.pedestal/pedestal.jetty "0.6.3"]
                 [org.slf4j/slf4j-nop "2.0.9"]
                 [org.slf4j/slf4j-api "2.0.9"]
                 [clj-http "3.12.3"]]
  :repl-options {:init-ns fs-components.core})