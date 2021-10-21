(ns versiontracker.db.data
  (:require [versiontracker.db.core :as db]
            [versiontracker.config :as config]
            [versiontracker.db.datacheck :as db-check]
            [versiontracker.config :refer [env]]
            [clj-http.client :as client]
            [clojure.data.json :as json]
            [clojure.tools.logging :as log]
            [clojure.string :as str]))

(defn db-type [] (keyword (second (str/split (env :database-url) #":"))))
(declare add-source!)
(declare add-target!)
(declare auto-enter-links)

;; Environments
(defn add-environment!
  "Adds new environment to database
   env - Should be a map with :name (required) and :comment (optional)

   Aldonas novan medion al datumbazo
   env - Devus esti mapo kun :name (bezonata) kaj :comment (nedeviga)"
  [env]
  (try
    (let [ret-val (db/create-environment! env)
          env-id  (case (db-type)
                    :h2        (:id (first ret-val))
                    :sqlserver (int (:generated_keys (first ret-val))))]
      (log/info (str "Environment with name: " (:name env) " create with ID: " env-id))
      env-id)
    (catch Exception e
         (log/error (str "Error inserting to db " e))
         {:result (str "Error inserting to db " e)})))

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
  (let [env-id (db-check/exist_env env-name)
        nod-ids (db/get-active-nodes {:env_id env-id :name (:name node)})]
    (if (nil? env-id)
      {:result "Environment must exists before adding a node"}
      (try
          (let [ret-val (db/create-node! (merge {:env_id env-id} node))
                nod-id  (case (db-type)
                           :h2        (:id (first ret-val))
                           :sqlserver (int (:generated_keys (first ret-val))))]
             (log/info (str "Node with name: " (:name node) " create with ID: " nod-id " in environment: " env-name))
             (do
               (when-not (empty? nod-ids)
                 (auto-enter-links nod-id nod-ids))
               nod-id))
        (catch Exception e
               (log/error (str "Error inserting to db: " e))
               {:result (str "Error inserting to db: " e)})))))

(defn ret-nodes
  "Retrieves a list of nodes from an environment

   Rekuperas liston de nodoj el medio"
  [env-name date]
  (let [env-id (db-check/exist_env env-name)]
    (if (nil? env-id)
     {:result "Cannot find the environment for requested node"}
     (let [base {:db-type (db-type) :env_id env-id}
           params (if-not (nil? date)
                    (assoc base :date date)
                    base)]
       (db/get-nodes params)))))

;; SubNodes
(defn add-subnode!
  "Adds new subnode to a node

   Aldonas nuvon subnodon al nodo"
  [env-name nod-name nod-version subnode]
  (let [env-id (db-check/exist_env env-name)
        nod-id (db-check/exist_node env-id nod-name nod-version)]
    (if (nil? nod-id)
     {:result "Either Environment or Node does not exist"}
     (try
       (let [ret-val (db/create-subnode! (merge {:nod_id nod-id} subnode))
             sub-id  (case (db-type)
                       :h2 (:id (first ret-val))
                       :sqlserver (int (:generated_keys (first ret-val))))]
         (log/info (str "SubNode with name: " (:name subnode) " create with ID: " sub-id " in environment: " env-name))
         {:result "SubNode succesfully added"})
       (catch Exception e
              (log/error (str "Error inserting to db: " e))
        {:result (str "Error inserting to db: " e)})))))


(defn ret-subnodes
  "Retrieves a list of subnodes for a node

   Rekuperas liston de subnodoj el nodo"
  [env-name nod-name nod-version date]
  (let [env-id (db-check/exist_env env-name)
        nod-id (db-check/exist_node env-id nod-name nod-version)]
    (if (nil? nod-id)
     {:result "Either Environment or Node does not exist"}
     (let [base {:db-type (db-type) :nod_id nod-id}
           params (if-not (nil? date)
                    (assoc base :date date)
                    base)]
       (db/get-subnodes params)))))

(defn add-interface!
  "Adds a new interface to an environment in case it does not exist yet and returns its ID

  Aldonas novan interfacon al medio, se ĝi ankoraŭ ne ekzistas kaj redonas sian identigilon"
  [{:keys [env-id name version type deploymentdate comment]}]
  (try
       (let [ret-val (db/create-link! {:env_id env-id :name name :version version :type type :deploymentdate deploymentdate :comment comment})]
         (case (db-type)
           :h2 (:id (first ret-val))
           :sqlserver (int (:generated_keys (first ret-val)))))
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
                                      :nod_id nod-id
                                      :sub_id sub-id}))]
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
                                      :nod_id nod-id
                                      :sub_id sub-id}))]
    (if (nil? tar-in)
      {:result "Target-node could not be added; check if the link and node do exist"}
      {:result "Target succesfully added to the link"})))

(defn add-link!
  "Adds new link to an environment

   Aldonas novan ligon al medio"
  [env-name link]
  (let [env-id (db-check/exist_env env-name)]
    (if-not (int? env-id)
      {:result "Environment does not exist"}
      (let [lin-id (add-interface! (merge {:env-id env-id} link))
            source (:source link)
            sou-in (when-not (and (int? lin-id) (nil? source))
                     (add-source! env-id lin-id source))
            target (:target link)
            tar-in (when-not (and (int? lin-id) (nil? target))
                     (add-target! env-id lin-id target))]
       {:result "The link was succesfully added"}))))


(defn prepare-links
  "Convert the db result to the correct map

   Konvertu la db-rezulton al la ĝusta mapo"
  [link]
  {:linid (:linid link)
   :name (:name link)
   :type (:type link)
   :version (:version link)
   :deploymentdate (:deploymentdate link)
   :comment (:comment link)
   :insertdate (:insertdate link)
   :source {:id (:sid link)
            :Node (:sourcename link)
            :Version (:sourceversion link)
            :subid (:ssid link)
            :SubNode (:sourcesubnode link)
            :SubVersion (:sourcesubversion link)}
   :target {:id (:tid link)
            :Node (:targetname link)
            :Version (:targetversion link)
            :subid (:tsid link)
            :SubNode (:targetsubnode link)
            :SubVersion (:targetsubversion link)}})


(defn ret-links
  "Retrieves a list of links from an environment

   Rekuperas liston de ligoj el medio"
  [env-name date]
  (let [env-id (db-check/exist_env env-name)
        base {:db-type (db-type) :env_id env-id}
          params (if-not (nil? date)
                   (assoc base :date date)
                   base)]
    (map #(prepare-links %) (db/get-links params))))

(defn add-node-to-link!
  [side env-name link-name link-version link]
  (let [env-id (db-check/exist_env env-name)
        lin-id (db-check/exist_link env-id link-name link-version)]
    (if-not (int? lin-id)
      {:result "Please add the link first"}
      (case side
            :source (add-source! env-id lin-id link)
            :target (add-target! env-id lin-id link)))))

(defn auto-enter-links
  "Not sure yet if this function will be used
   It is prepared for updating a previous link, however do we need to update all links?"
  [nod-id nod-ids]
  (let [sources (db/get-active-sources-for-node {:nod_ids (mapv #(:id %) nod-ids)})
        targets (db/get-active-targets-for-node {:nod_ids (mapv #(:id %) nod-ids)})
        u-sours (map #(assoc % :nod_id nod-id) sources)
        u-targs (map #(assoc % :nod_id nod-id) targets)]
    nil))
