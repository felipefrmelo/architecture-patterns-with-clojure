(ns architecture-patterns-with-clojure.e2e.helper-api
  (:require [architecture-patterns-with-clojure.util.json :as json]
            [clojure.string :as str]
            [clojure.test :refer [is]]
            [io.pedestal.test :refer [response-for]]))

(defn format-url [url]
  (if (str/starts-with? url "/") url (str "/" url)))

(defn make-request [service method url & {:keys [body]}]
  (response-for service method (format-url url)
                :headers {"Content-Type" "application/json"}
                :body (json/stringify body)
                ))

(defn get-allocation [service order_id]
  (make-request service :get (str "/allocations/" order_id)))

(defn post-to-add-batch! [service ref sku quantity eta]
  (is (= (:status (make-request service :post "/batch"
                                :body {:sku      sku
                                       :quantity quantity
                                       :ref      ref
                                       :eta      eta}))
         201)))

(defn post-to-allocate!
  ([service order_id sku quantity] (post-to-allocate! service order_id sku quantity 201))
  ([service order_id sku quantity expected_code]
   (let [res (make-request service :post "/allocate"
                           :body {:order_id order_id
                                  :sku      sku
                                  :quantity quantity})]
     (is (= (:status res) expected_code))
     res)))
