(ns architecture-patterns-with-clojure.util.date
  (:import
    (java.time.temporal ChronoUnit)
    (java.time LocalDateTime ZoneId LocalDate)
    (java.util Date))
  )

(def DAYS (ChronoUnit/DAYS))
(def MINUTES (ChronoUnit/MINUTES))


(def now #(LocalDateTime/now))
(defn plus [date amount-to-add unit]
  (.plus date amount-to-add unit))

(defn minus [date amount-to-add unit] (plus date amount-to-add unit))

(defn from-db [date]
  (-> date (.toInstant) (.atZone (ZoneId/systemDefault)) (.toLocalDateTime)))

(defn to-db [date] (-> date (.atZone (ZoneId/systemDefault)) .toInstant Date/from))

(defn parse [date-string] (LocalDateTime/parse date-string))

(defn to-inst-ms [date]
  (-> date (.atZone (ZoneId/systemDefault)) (.toInstant) (inst-ms)))

