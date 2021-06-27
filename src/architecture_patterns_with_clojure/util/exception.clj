(ns architecture-patterns-with-clojure.util.exception)


(defn make-ex-info [{:keys [message data]}]
  (ex-info message {:errors (into [] data)}))
