(ns versiontracker.db.datacheck
  (:require [versiontracker.db.core :as db]
            [versiontracker.config :as config]
            [clj-http.client :as client]
            [clojure.data.json :as json]
            [clojure.tools.logging :as log]))

(defn exist_env?
  "Check if the environemnt exists in the database"
  [env-name]
  (if (= 1 (count (db/get-environment {:env_name env-name})))
    true
    false))

(defn exist_node?
  "Check if the environemnt exists in the database"
  [env-name nod-name nod-version]
  (if (= 1 (count (db/get-node {:env_name env-name
                                :nod_name nod-name
                                :nod_version nod-version})))
    true
    false))

(defn exist_subnode?
  "Check if the environemnt exists in the database"
  [env-name nod-name nod-version sub-name sub-version]
  (if (nil? sub-name)
    true
    (if (= 1 (count (db/get-subnode {:env_name env-name
                                     :nod_name nod-name
                                     :nod_version nod-version
                                     :sub_name sub-name
                                     :sub_version sub-version})))
      true
      false)))
