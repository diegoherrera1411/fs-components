(ns fs-components.utils.files
  (:require [clojure.edn :as edn]))

(defn parse-edn-file [filename]
  (edn/read-string (slurp filename)))

(defn config-filepath [project-name env]
  (str "resources/config/" project-name "-" (name env) ".edn"))
