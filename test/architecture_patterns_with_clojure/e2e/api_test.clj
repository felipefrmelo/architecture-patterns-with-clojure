(ns architecture-patterns-with-clojure.e2e.api-test
  (:require [clojure.test :refer [deftest is use-fixtures]]
            [io.pedestal.test :refer [response-for]]
            [io.pedestal.http :as bootstrap]
            [clojure.string :as str]
            [architecture-patterns-with-clojure.random-refs :refer [random-sku random-batchref random-orderid]]
            [architecture-patterns-with-clojure.util.json :as json]
            [architecture-patterns-with-clojure.entrypoints.api :as api])
  )

(def service
  (::bootstrap/service-fn (bootstrap/create-servlet api/service)))

(defn format-url [url]
  (if (str/starts-with? url "/") url (str "/" url)))

(defn make-request [method url & {:keys [body]}]
  (response-for service method (format-url url)
                :headers {"Content-Type" "application/json"}
                :body (json/stringify body)
                ))

(defn post-to-add-batch! [ref sku quantity eta]
  (is (= (:status (make-request :post "/batch"
                                :body {:sku      sku
                                       :quantity quantity
                                       :ref      ref
                                       :eta      eta}))
         201)))

(defn post-to-allocate!
  ([order_id sku quantity] (post-to-allocate! order_id sku quantity 201))
  ([order_id sku quantity expected_code]
   (let [res (make-request :post "/allocate"
                           :body {:order_id order_id
                                  :sku      sku
                                  :quantity quantity})]
     (is (= (:status res) expected_code))
     res)))

(deftest happy-path-returns-201-and-allocated-batch
  (let [order_id (random-orderid)
        sku (random-sku)
        early-batch (random-batchref :name 1)
        later-batch (random-batchref :name 2)
        other-batch (random-batchref :name 3)]
    (post-to-add-batch! early-batch sku 10 "2011-01-01T00:00")
    (post-to-add-batch! later-batch sku 10 "2011-01-02T00:00")
    (post-to-add-batch! other-batch sku 10 "2011-01-02T00:00")

    (is (= order_id (get-in (post-to-allocate! order_id sku 10) [:headers "Location"])))
    )
  )

(deftest unhappy-path-returns-400-and-error-message
  (let [order_id (random-orderid)
        unknown_sku (random-sku)
        ]
    (is (= (str "Invalid sku " unknown_sku)
           (-> (post-to-allocate! order_id unknown_sku 10 400)
               :body json/parse :msg)))
    )
  )

