(ns metabase.driver.rest
  "REST API driver"
  (:require
   [metabase.driver :as driver]
   [metabase.driver.rest.client :as rest.client]))

(driver/register! :rest)

(doseq [[feature supported?] {:native-parameters                      true
                              :convert-timezone                       false
                              :basic-aggregations                     false
                              :case-sensitivity-string-filter-options false
                              :date-arithmetics                       false
                              :temporal-extract                       false
                              :schemas                                false
                              :test/jvm-timezone-setting              false
                              :fingerprint                            false
                              :upload-with-auto-pk                    false}]
  (defmethod driver/database-supports? [:rest feature] [_driver _feature _db] supported?))

(defmethod driver/can-connect? :rest
  [_ _details]
  true)

(defmethod driver/display-name :rest [_] "REST API")

(defmethod driver/describe-table :rest
  [_ database table]
  (rest.client/describe-table database table))

(defmethod driver/describe-database :rest
  [_ database]
  (rest.client/describe-database database))

(defmethod driver/mbql->native :rest
  [_ query]
  (rest.client/mbql->native query))

(defmethod driver/execute-reducible-query :rest
  [_driver query _context respond]
  (let [[results-metadata rows] (rest.client/execute-query query)]
    (respond results-metadata rows)))

(defmethod driver/substitute-native-parameters :rest
  [_driver inner-query]
  (rest.client/substitute-native-parameters inner-query))
