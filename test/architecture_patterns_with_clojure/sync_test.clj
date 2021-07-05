(ns architecture-patterns-with-clojure.sync-test
  (:require [clojure.test :refer :all])
  (:require [architecture-patterns-with-clojure.sync :as sync-f]))


(deftest determine-actions

  (testing "when a file exists in the source but not the destination"
    (let [source_dash {:hash1 "fn1"}
          dest_hash {}
          actions (sync-f/determine-actions source_dash dest_hash "/src" "/dst")]
      (is (=  ['("COPY", "/src/fn1" "/dst/fn1")] actions))
      ))

  (testing "when a file has been renamed in the source"
    (let [source_dash {"hash1" "fn1"}
          dest_hash {"hash1" "fn2"}
          actions (sync-f/determine-actions source_dash dest_hash "/src" "/dst")]
      (is (=  ['("MOVE", "/dst/fn2" "/dst/fn1")] actions))
      ))
  )