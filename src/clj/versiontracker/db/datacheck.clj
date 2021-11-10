(ns versiontracker.db.datacheck
  (:require [versiontracker.db.core :as db]
            [versiontracker.config :as config]
            [clj-http.client :as client]
            [clojure.data.json :as json]
            [clojure.tools.logging :as log]))

(defn exist_env
  "Returns ID when the environment exists in the database otherwise 'nil'

  Liveras ID kiam la medio ekzistas en la datumbazo alie 'nil'"
  [name]
  (log/info (str "Exist Environment | Checking if the environment: " name " exists"))
  (:id (db/get-environment-id {:env_name name})))

(defn exist_node
  "Returns ID when the environment, node with node version combination exists in the database otherwies 'nil'

  Liveras ID kiam la medio, la nodo kun la nodo versio ekzistas en la datumbazo alie 'nil'"
  [env-id name version]
  (log/info (str "Exist Node | Checking if the node: " name " - " version " exists within environment with id: " env-id))
  (:id (db/get-node-id {:env_id env-id
                        :name name
                        :version version})))


(defn exist_subnode
  "Returns ID when the subnode with version exists in the database for a certain node otherwise 'nil'

  Liveras ID kiam la subnodo kun versio ekzistas en la datumbazo por certa nodo alie 'nil'"
  [nod-id name version]
  (log/info (str "Exist SubNode | Checking if the subnode: " name " - " version " exists within node with id: " nod-id))
  (:id (db/get-subnode-id {:nod_id nod-id
                           :name name
                           :version version})))

(defn exist_link
  "Returns ID when the link with version exists in the database otherwise 'nil'

  Liveras ID kiam la ligo kun versio ekzistas en la datumbazo alie 'nil'"
  [env-id name version]
  (log/info (str "Exist Link | Checking if the link: " name " - " version " exists within environment with id: " env-id))
  (:id (db/get-link-id {:env_id env-id
                        :name name
                        :version version})))
