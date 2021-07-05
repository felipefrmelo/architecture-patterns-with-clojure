(ns architecture-patterns-with-clojure.sync
  (:require [clojure.java.io :as io])
  (:import (java.security MessageDigest))
  )


(defn hash-file [path]
  (->> (.getBytes (slurp path) "utf8")
       (.digest (MessageDigest/getInstance "SHA1"))
       (BigInteger. 1)
       (format "%x")))


(hash-file "src/architecture_patterns_with_clojure/test.txt")




(defn read_paths_and_hashes [root]
  (->> root
       (io/file)
       (file-seq)
       (reduce
         (fn [hashes, file]
           (if (.isDirectory file)
             hashes
             (assoc hashes (keyword (hash-file file)) (.getName file)))) {})))


(defn determine-actions [source_hashes dest_hashes source_folder dest_folder]

  (-> (reduce-kv (fn [actions sha filename]
                   (cond
                     (nil? (get dest_hashes sha)) (conj actions (let [[source_path dest_path] (for [file [filename]
                                                                                                    path [source_folder dest_folder]]
                                                                                                (str (io/file path file)))]
                                                                  (list "COPY" source_path dest_path)))
                     (not= (get dest_hashes sha) filename)
                     (conj actions (let [[olddest_path newdest_path] (for [path [dest_folder]
                                                                           file [(get dest_hashes sha) filename]]
                                                                       (str (io/file path file)))]
                                     (list "MOVE" olddest_path newdest_path))

                           )
                     :default actions)
                   ) [] source_hashes)
      (concat (reduce-kv (fn [actions sha filename] (if (nil? (get source_hashes sha))
                                                      (list "DELETE" (str (io/file dest_folder filename)))
                                                      actions)
                           ) [] dest_hashes)))

  )


