(ns architecture-patterns-with-clojure.util.date
  (:import
    (java.time.temporal ChronoUnit)
    (java.time ZonedDateTime)
    (java.util Date Calendar)))

(def units {:minutes (ChronoUnit/MINUTES)
            :days    (ChronoUnit/DAYS)})


(def now #(Date.))
(defn plus [date amount-to-add unit]
  (-> date (.toInstant) (.plus  amount-to-add (units unit)) (Date/from))
  )
(defn minus [date amount-to-add unit] (plus date amount-to-add unit))

