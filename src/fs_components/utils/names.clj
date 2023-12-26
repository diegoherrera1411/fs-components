(ns fs-components.utils.names
  (:require [clojure.main :as main]
            [clojure.string :as clj-str])
  (:import (java.net URLEncoder)))

(defn fn-name [fn-object]
  (as-> (str fn-object) $
        (main/demunge $)
        (or (re-find #"(.+)--\d+@" $)
            (re-find #"(.+)@" $))
        (last $)))

(defn http-op-url [base-url url path-params]
  (let [raw-url (clj-str/join "" [base-url url])]
    (reduce
      (fn [new-url [param-kw param-val]]
        (clj-str/replace new-url (str param-kw) (URLEncoder/encode (str param-val))))
      raw-url
      path-params)))
