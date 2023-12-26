(ns fs-components.utils.environment)

(def valid-env #{:prod :dev :test :local})

(defn valid-env? [env]
  (boolean (valid-env env)))

(defn is-env? [env-type obj]
  (= (:env obj) env-type))

(def prod? (partial is-env? :prod))
(def dev? (partial is-env? :dev))
(def test? (partial is-env? :test))
(def local? (partial is-env? :local))

(def not-test? (comp not test?))