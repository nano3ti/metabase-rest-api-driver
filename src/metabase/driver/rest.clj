(ns metabase.driver.rest
  "REST API driver"
  (:require
   [metabase.driver :as driver]
   [metabase.driver.rest.client :as rest.client]))

(driver/register! :rest)

(doseq [[feature supported?] {:expression-aggregations false
                              :schemas                 false
                              :set-timezone            false}]
  (defmethod driver/database-supports? [:rest feature] [_driver _feature _db] supported?))

(defmethod driver/can-connect? :rest
  [_ details]
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
