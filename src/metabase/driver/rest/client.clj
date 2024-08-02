(ns metabase.driver.rest.client
  "REST API driver"
  (:require
   [cheshire.core :as json]
   [clj-http.client :as http]
   [clojure.string :as string]
   [metabase.lib.metadata :as lib.metadata]
   [metabase.query-processor.store :as qp.store]))

(defn- make-headers [details]
  (let [token (:auth-token details)]
    (if (and token (not (string/blank? token)))
      {:authorization (str "Bearer " token)}
      {})))

(defn- fetch-table-defs [database]
  (let [details (:details database)
        db-info-path (:db-info-path details)]
    (when db-info-path
      (let [url (str (:url details) db-info-path)
            response (http/request {:url url
                                    :method :get
                                    :headers (make-headers details)
                                    :as :json})
            status (:status response)]
        (when (not= 200 status)
          (throw (Exception. "Table defs request error: " status)))
        (let [resp-data (:body response)]
          (:tables resp-data))))))

(defn- get-table-defs [database]
  (let [table-defs (:tables (:details database))
        parsed-table-defs (json/parse-string table-defs true)]
    (concat
     (or parsed-table-defs [])
     (or (fetch-table-defs database) []))))

(defn- get-table-def [database table-name]
  (let [table-defs (get-table-defs database)]
    (first (filter
            (fn [table-def]
              (= table-name (:name table-def)))
            table-defs))))

(defn- parse-dtype
  [dtype]
  (keyword (str "type/" dtype)))

(defn describe-database
  [database]
  (let [table-defs (get-table-defs database)
        tables-meta (map (fn [table-def]
                           {:name (:name table-def)
                            :schema nil}) table-defs)]
    {:tables (set tables-meta)}))

(defn- parse-fields
  [fields]
  (map-indexed (fn [idx field] {:name (:name field)
                                :database-type (:dtype field)
                                :base-type (parse-dtype (:dtype field))
                                :database-position idx})
               fields))

(defn describe-table
  [database table]
  (let [table-name (:name table)
        table-def (get-table-def database table-name)]
    (when table-def
      {:name   table-name
       :schema nil
       :fields (set (parse-fields (:columns table-def)))})))

(defn mbql->native [query]
  (let [{table :name} (lib.metadata/table
                       (qp.store/metadata-provider)
                       (:source-table (:query query)))]
    {:table table}))

(defn- parse-method [method]
  (when method
    (keyword (string/lower-case method))))

(defn- query-api
  [database api-query]
  (let [details (:details database)
        body (:body api-query)
        url (str (:url details) (:path api-query))
        response (http/request {:url url
                                :method (or (parse-method (:method api-query)) :get)
                                :headers (merge
                                          (make-headers details)
                                          {:content-type "application/json;charset=UTF-8"}
                                          (or (:headers api-query) {}))
                                :body (when body (json/generate-string body))
                                :as :json})
        status (:status response)]
    (when (not= 200 status)
      (throw (Exception. "API request error: " status)))
    (let [resp-data (:body response)
          columns (:columns (:info resp-data))
          rows (:rows resp-data)]
      [{:cols (map (fn [col] {:name (:name col)}) columns)}
       rows])))

(defn- fetch-predefined-table
  [database table-name]
  (let [table-def (get-table-def database table-name)]
    (when table-def
      (query-api database (:query table-def)))))

(defn execute-query
  [query]
  (let [native-query (:native query)
        api-query (:query native-query)
        database (lib.metadata/database (qp.store/metadata-provider))]
    (if api-query
      (query-api database (json/parse-string api-query true))
      (fetch-predefined-table database (:table native-query)))))

(defn- substitute-parameters [parameters query-string]
  (reduce (fn [qs {value :value [_ [_ template-tag]] :target}]
            (string/replace qs (str "{{" template-tag "}}") (str value)))
          query-string parameters))

(defn substitute-native-parameters [inner-query]
  (let [parameters (:parameters inner-query)
        query-string (:query inner-query)]
    (if (and (seq parameters) query-string)
      {:query (substitute-parameters parameters query-string)}
      {:query query-string})))
