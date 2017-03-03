(ns restaurant-inspections-api.services
  (:require [yesql.core :refer [defqueries]]
            [restaurant-inspections-api.db :as db]
            [restaurant-inspections-api.util :as util]
            [taoensso.timbre :refer [debug]]
            [clojure.string :as str]))

(defn format-data
  "Format db raw data to json."
  ([data]
   (format-data data false))
  ([data is-full]
   (assoc (if is-full
            data
            (dissoc data
                    :critical_violations_before_2013
                    :noncritical_violations_before_2013
                    :license_id
                    :violations))
          :id (:inspection_visit_id data)
          :inspection_date (util/parse-date-or-nil (:inspection_date data)))))

(defn violations-for-inspection
  "Select and parse violations for a given inspection id."
  [inspection-id]
  (map (fn [violation]
         {:id               (:violation_id violation)
          :count            (:violation_count violation)
          :description      (:description violation)
          :isRiskFactor     (:is_risk_factor violation)
          :isPrimaryConcern (:is_primary_concern violation)})
       (db/select-violations-by-inspection {:id inspection-id})))

(defn full-inspection-details
  "Return full inspection info for the given Id."
  [id]
  (if-let [inspection (first (db/select-inspection-details {:id id}))]
    (format-data (assoc inspection :violations (violations-for-inspection (:inspection_visit_id inspection)))
                 true)))

(defn full-business-details
  "Return full business info for the given Id."
  [id]
  (db/select-restaurant-details {:licenseNumber id}))

(defn format-params
  "Format all the pre-params sent to this endpoint"
  [params-map]
  (assoc params-map
    :businessName (when-let [businessName (:businessName params-map)]
                    (clojure.string/replace businessName #"\*" "%"))
    :zipCodes (when-let [zipCodes (:zipCodes params-map)]
                (str/split zipCodes #","))
    :page (* (:page params-map) (:perPage params-map))))

(defn inspections-by-all
  "Retrieves and formats inspections, filtered by all, any, or no criteria."
  [params-map]
  (map format-data (db/select-inspections-by-all (format-params params-map))))

(defn get-counties
  "Return counties list, with their district."
  []
  (db/select-counties-summary))

(defn- format-businesses-params
  [params-map]
  (assoc params-map
         :zipCodes (when-let [zipCodes (:zipCodes params-map)]
                     (str/split zipCodes #","))
         :page (* (:page params-map) (:perPage params-map))))

(defn get-businesses
  ""
  [params-map]
  (db/select-all-restaurants (format-businesses-params params-map)))

(defn get-violations
  ""
  []
  (db/select-all-violations))
