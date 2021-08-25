(ns versiontracker.db.data
  (:require [versiontracker.db.core :as db]
            [versiontracker.config :as config]
            [clj-http.client :as client]
            [clojure.data.json :as json]
            [clojure.tools.logging :as log]))

(def vt-config (config/get-configuration))

;; Environments
(defn add-environment!
  "Adds new environment to database"
  [env]
  (if (nil? env)
    {:result "Can't add an empty environment"}
    (try
      (db/create-environment! {:name (:name env) :comment (:comment env)})
      (catch Exception e
             (log/error (str "Error inserting to db: " e))
             {:result (str "Error inserting to db: " e)}))))

(defn ret-environments
  "Retrieves the list of environments"
  []
  (db/get-environments))

(defn ret-environment
  "Retrieves the dat either from API, file or db"
  [env-name]
  (if (nil? env-name)
    {:result "You need to specify the environment name"}
    (try
      (first (db/get-environment {:env_name env-name}))
      (catch Exception e
             (log/error (str "Error getting from db: " e))
             {:result (str "Error getting from db: " e)}))))

;; Nodes
(defn add-node!
  "Adds new node to an environment"
  [env-name node]
  (if (or (nil? env-name) (nil? node))
      {:result "Neither the environment name nor the node map can be empty"}
      (try
        (db/create-node! {:env_name env-name
                          :name (:name node)
                          :type (:type node)
                          :version (:version node)
                          :deploymentdate (:deploymentdate node)
                          :comment (:comment node)})
        (catch Exception e
               (log/error (str "Error inserting to db: " e))
               {:result (str "Error inserting to db: " e)}))))

(defn prepare-nodes
  "Convert the db result to the correct map"
  [node]
  {:name (:name node)
   :type (:type node)
   :version (:version node)
   :deploymentdate (str (:depdate node))
   :comment (:comment node)})


(defn ret-nodes
  "Retrieves a list of nodes from an environment"
  [env-name date]
  (let [base {:env_name env-name}
        params (if-not (nil? date)
                 (assoc base :date date)
                 base)]
    (mapv #(prepare-nodes %) (db/get-nodes params))))

;; SubNodes
(defn add-subnode!
  "Adds new subnode to a node"
  [env-name nod-name nod-version subnode]
  (if (or (nil? env-name) (nil? nod-name) (nil? nod-version) (nil? subnode))
    {:result "Empty parameters are not allowed"}
    (let [sub-in (try
                   (db/create-subnode! {:env_name env-name
                                        :nod_name nod-name
                                        :nod_version nod-version
                                        :name (:name subnode)
                                        :version (:version subnode)
                                        :deploymentdate (:deploymentdate subnode)
                                        :comment (:comment subnode)})
                   (catch Exception e
                          (log/error (str "Error inserting to db: " e))
                          {:result (str "Error inserting to db: " e)}))]
      {:result "SubNode succesfully added"})))

(defn ret-subnodes
  "Retrieves a list of subnodes for a node"
  [env-name nod-name nod-version date]
  (let [base {:env_name env-name :nod_name nod-name :nod_version nod-version}
        params (if-not (nil? date)
                 (assoc base :date date)
                 base)]
    (db/get-subnodes params)))

(defn prepare-link-params
  "creates the params for link insertion"
  [env-name link]
  {:env_name env-name
   :name (:name link)
   :type (:type link)
   :version (:version link)
   :deploymentdate (:deploymentdate link)
   :comment (:comment link)})

(defn add-link!
  "Adds new link to an environment"
  [env-name link]
  (if (or (nil? env-name) (nil? link))
    {:result "Empty parameters are not allowed"}
    (let [params (prepare-link-params env-name link)
          lin-in (db/create-link! params)
          lin-id (:id (db/get-link-id {:env_name env-name
                                       :name (:name link)
                                       :version (:version link)}))
          source (:source link)
          sou-in (if-not (nil? source)
                   (db/create-source! {:lin_id lin-id
                                       :nod_name (:Node source) :nod_version (:Version source)
                                       :sub_name (:SubNode source) :sub_version (:SubVersion source)})
                   0)
          target (:target link)
          tar-in (if-not (nil? target)
                   (db/create-target! {:lin_id lin-id
                                       :nod_name (:Node target) :nod_version (:Version target)
                                       :sub_name (:SubNode target) :sub_version (:SubVersion target)})
                   0)
          result (str lin-in sou-in tar-in)]
      (case result
            "100" {:result "The link with neither source nor target was added"}
            "110" {:result "The link with source and without target was added"}
            "101" {:result "The link without source and with target was added"}
            "111" {:result "The link with both source and target was added"}))))


(defn prepare-links
  "Convert the db result to the correct map"
  [link]
  {:name (:name link)
   :type (:type link)
   :version (:version link)
   :deploymentdate (str (:depdate link))
   :comment (:comment link)
   :insertdate (:insertdate link)
   :source {:Node (:sourcename link)
            :Version (:sourceversion link)
            :SubNode (:sourcesubnode link)
            :SubVersion (:sourcesubversion link)}
   :target {:Node (:targetname link)
            :Version (:targetversion link)
            :SubNode (:targetsubnode link)
            :SubVersion (:targetsubversion link)}})


(defn ret-links
  "Retrieves a list of links from an environment"
  [env-name date]
  (let [base {:env_name env-name}
        params (if-not (nil? date)
                 (assoc base :date date)
                 base)]
    (map #(prepare-links %) (db/get-links params))))
    ; (println links)))

(defn add-source!
  "Adds new source to a link in the environment"
  [env-name link-name link-version source]
  (if (or (nil? env-name) (nil? link-name) (nil? link-version) (nil? source))
    {:result "Empty parameters are not allowed"}
    (let [lin-id (:id (db/get-link-id {:env_name env-name
                                       :name link-name
                                       :version link-version}))
          sou-in (db/create-source! {:lin_id lin-id
                                     :nod_name (:Node source) :nod_version (:Version source)
                                     :sub_name (:SubNode source) :sub_version (:SubVersion source)})]
      {:result "Source succesfully added to the link"})))

(defn add-target!
  "Adds new target to a link in the environment"
  [env-name link-name link-version target]
  (if (or (nil? env-name) (nil? link-name) (nil? link-version) (nil? target))
    {:result "Empty parameters are not allowed"}
    (let [lin-id (:id (db/get-link-id {:env_name env-name
                                       :name link-name
                                       :version link-version}))
          tar-in (db/create-target! {:lin_id lin-id
                                     :nod_name (:Node target) :nod_version (:Version target)
                                     :sub_name (:SubNode target) :sub_version (:SubVersion target)})]
      {:result "Target succesfully added to the link"})))
