(ns fs-components.config.protocol)

(defprotocol Config
  "Interface for interacting with a config object"
  (get! [config path] "Gets a config value in a particular path in the config object"))