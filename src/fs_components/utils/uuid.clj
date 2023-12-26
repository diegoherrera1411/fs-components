(ns fs-components.utils.uuid
  (:import (java.util UUID)))

(defn new-uuid []
  (UUID/randomUUID))
