(ns versiontracker.db.datacheck
  (:require [versiontracker.db.core :as db]
            [versiontracker.config :as config]
            [clj-http.client :as client]
            [clojure.data.json :as json]
            [clojure.tools.logging :as log]))

(defn exist_env?
  "Returns 'true' when the environment exists in the database

  Liveras 'true' kiam la medio ekzistas en la datumbazo"
  [env-name]
  (if (= 1 (count (db/get-environment {:env_name env-name})))
    true
    false))

(defn exist_node?
  "Returns 'true' when the environment, node with node version combination exists in the database

  Liveras 'true' kiam la medio, la nodo kun la nodo versio ekzistas en la datumbazo"
  [env-name nod-name nod-version]
  (let [nodes (count (db/get-node {:env_name env-name
                                   :nod_name nod-name
                                   :nod_version nod-version}))]
    (= 1 nodes)))


(defn exist_subnode?
  "Returns 'true' when requested with subnode name is 'nil' or when the environment,
  node with version and subnode with version exists in the database

  Liveras 'true' kiam petite kun la nomo de subnodo estas 'nil' aŭ kiam la medio,
   nodo kun versio kaj subnodo kun versio ekzistas en la datumbazo"
  [env-name nod-name nod-version sub-name sub-version]
  (let [subnodes
        (if (nil? sub-name)
          1
          (count (db/get-subnode {:env_name env-name
                                  :nod_name nod-name
                                  :nod_version nod-version
                                  :sub_name sub-name
                                  :sub_version sub-version})))]
    (= 1 subnodes)))
