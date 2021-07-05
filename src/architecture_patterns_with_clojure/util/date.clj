(ns architecture-patterns-with-clojure.util.date
  (:import
    (java.time.temporal ChronoUnit)
    (java.time LocalDateTime ZoneId)
    (java.util Date))
  )

(def ^:const ^:unit DAYS (ChronoUnit/DAYS))
(def ^:const ^:unit MINUTES (ChronoUnit/MINUTES))


(def now #(LocalDateTime/now))
(defn plus [date amount-to-add unit]
  (.plus date amount-to-add unit))

(defn minus [date amount-to-add unit] (plus date amount-to-add unit))

(defn from-db [date]
  (-> date (.toInstant) (.atZone (ZoneId/systemDefault)) (.toLocalDateTime)))

(defn to-db [date] (identity date))

(defn to-inst-ms [date]
  (-> date (.atZone (ZoneId/systemDefault)) (.toInstant) (inst-ms)))

