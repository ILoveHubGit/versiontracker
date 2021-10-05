(ns versiontracker.db.data
  (:require [versiontracker.db.core :as db]
            [versiontracker.config :as config]
            [versiontracker.db.datacheck :as db-check]
            [clj-http.client :as client]
            [clojure.data.json :as json]
            [clojure.tools.logging :as log]))

; (def vt-config (config/get-configuration))
(declare add-source!)
(declare add-target!)

;; Environments
(defn add-environment!
  "Adds new environment to database
   env - Should be a map with :name (required) and :comment (optional)

   Aldonas novan medion al datumbazo
   env - Devus esti mapo kun :name (bezonata) kaj :comment (nedeviga)"
  [env]
  (if (nil? (:name env))
    (do
      (log/info (str "Environment name is missing, can't add environment"))
      {:result "Can't add an empty environment"})
    (try
      (let [env-id (:id (first (db/create-environment! {:name (:name env) :comment (:comment env)})))]
        (log/info (str "Environment with name: " (:name env) " create with ID: " env-id))
        env-id)
      (catch Exception e
             (log/error (str "Error inserting to db: " e))
             {:result (str "Error inserting to db: " e)}))))

(defn ret-environments
  "Retrieves the list of environments

   Rekuperas la liston de medioj"
  []
  (db/get-environments))

(defn ret-environment
  "Retrieves an environment

   Rekuperas medio"
  [env-name]
  (if (nil? env-name)
    {:result "You need to specify the environment name"}
    (try
      (db/get-environment {:env_name env-name})
      (catch Exception e
             (log/error (str "Error getting from db: " e))
             {:result (str "Error getting from db: " e)}))))

;; Nodes
(defn add-node!
  "Adds new node to an environment

   Aldonas novan nodon al medio"
  [env-name node]
  (let [env-id (db-check/exist_env env-name)]
    (if (nil? env-id)
        {:result "Environment must exists before adding a node"}
        (try
          (let [nod-id (:id (first (db/create-node! (merge {:env_id env-id} node))))]
             (log/info (str "Node with name: " (:name node) " create with ID: " nod-id " in environment: " env-name))
             nod-id)
          (catch Exception e
                 (log/error (str "Error inserting to db: " e))
                 {:result (str "Error inserting to db: " e)})))))

(defn prepare-nodes
  "Convert the db result to the correct map

   Konvertu la db-rezulton al la ĝusta mapo"
  [node]
  {:name (:name node)
   :type (:type node)
   :version (:version node)
   :deploymentdate (str (:depdate node))
   :comment (:comment node)})


(defn ret-nodes
  "Retrieves a list of nodes from an environment

   Rekuperas liston de nodoj el medio"
  [env-name date]
  (let [env-id (db-check/exist_env env-name)]
    (if (nil? env-id)
      {:result "Cannot find the environment for requested node"}
      (let [base {:env_id env-id}
            params (if-not (nil? date)
                     (assoc base :date date)
                     base)]
        (mapv #(prepare-nodes %) (db/get-nodes params))))))

;; SubNodes
(defn add-subnode!
  "Adds new subnode to a node

   Aldonas nuvon subnodon al nodo"
  [env-name nod-name nod-version subnode]
  (let [env-id (db-check/exist_env env-name)
        nod-id (db-check/exist_node env-id nod-name nod-version)]
    (if (nil? nod-id)
      {:result "Either Environment or Node does not exist"}
      (let [sub-in (try
                     (let [sub-id (:id (first (db/create-subnode! (merge {:nod_id nod-id} subnode))))]
                       (log/info (str "SubNode with name: " (:name subnode) " create with ID: " sub-id " in environment: " env-name))
                       nod-id)
                     (catch Exception e
                            (log/error (str "Error inserting to db: " e))
                            {:result (str "Error inserting to db: " e)}))]
        {:result "SubNode succesfully added"}))))

(defn ret-subnodes
  "Retrieves a list of subnodes for a node

   Rekuperas liston de subnodoj el nodo"
  [env-name nod-name nod-version date]
  (let [env-id (db-check/exist_env env-name)
        nod-id (db-check/exist_node env-id nod-name nod-version)]
    (if (nil? nod-id)
      {:result "Either Environment or Node does not exist"}
      (let [base {:nod_id nod-id}
            params (if-not (nil? date)
                     (assoc base :date date)
                     base)]
        (db/get-subnodes params)))))

(defn add-interface!
  "Adds a new interface to an environment in case it does not exist yet and returns its ID

  Aldonas novan interfacon al medio, se ĝi ankoraŭ ne ekzistas kaj redonas sian identigilon"
  [{:keys [env-id name version type deploymentdate comment]}]
  (try
       (:id (first (db/create-link!)))
       (catch Exception e
              (log/error (str "Error inserting link to db: " e))
              {:result (str "Error inserting link to db: " e)})))

(defn add-source!
  "Adds new source to a link in the environment

   Aldonas novan fonto al ligo en la medio"
  [env-id lin-id {:keys [Node Version SubNode SubVersion]}]
  (let [nod-id  (db-check/exist_node env-id Node Version)
        sub-id  (db-check/exist_subnode nod-id SubNode SubVersion)
        sou-in  (when-not (nil? nod-id)
                  (db/create-source! {:lin_id lin-id
                                      :nod_name Node :nod_version Version
                                      :sub_name SubNode :sub_version SubVersion}))]
    (if (nil? sou-in)
      {:result "Source-node could not be added; check if the link and node do exist"}
      {:result "Source succesfully added to the link"})))

(defn add-target!
  "Adds new target to a link in the environment

   Aldonas novan celo al ligo en la medio"
  [env-id lin-id {:keys [Node Version SubNode SubVersion]}]
  (let [nod-id  (db-check/exist_node env-id Node Version)
        sub-id  (db-check/exist_subnode nod-id SubNode SubVersion)
        tar-in  (when-not (nil? nod-id)
                  (db/create-target! {:lin_id lin-id
                                      :nod_name Node :nod_version Version
                                      :sub_name SubNode :sub_version SubVersion}))]
    (if (nil? tar-in)
      {:result "Target-node could not be added; check if the link and node do exist"}
      {:result "Target succesfully added to the link"})))

(defn add-link!
  "Adds new link to an environment

   Aldonas novan ligon al medio"
  [env-name link]
  (let [env-id (db-check/exist_env env-name)]
    (if (nil? env-id)
      {:result "Empty parameters are not allowed"}
      (let [lin-id (add-interface! (merge {:env_id env-id} link))
            source (:source link)
            sou-in (when-not (nil? source)
                     (add-source! env-id lin-id source))
            target (:target link)
            tar-in (when-not (nil? target)
                     (add-target! env-id lin-id target))]
       {:result "The link was succesfully added"}))))


(defn prepare-links
  "Convert the db result to the correct map

   Konvertu la db-rezulton al la ĝusta mapo"
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
  "Retrieves a list of links from an environment

   Rekuperas liston de ligoj el medio"
  [env-name date]
  (let [env-id (db-check/exist_env env-name)
        base {:env_id env-id}
        params (if-not (nil? date)
                 (assoc base :date date)
                 base)]
    (map #(prepare-links %) (db/get-links params))))

(defn add-node-to-link!
  [side env-name link-name link-version link]
  (let [env-id (db-check/exist_env env-name)
        lin-id (db-check/exist_link env-id link-name link-version)]
    (if (nil? lin-id)
      {:result "Please add the link first"}
      (case side
            :source (add-source! env-id lin-id link)
            :target (add-target! env-id lin-id link)))))
