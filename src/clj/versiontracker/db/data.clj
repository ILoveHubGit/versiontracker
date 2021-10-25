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

(defn updateVersions
  [keepVersions k-id v-id ids id-type & {id-func :id-func
                                         source-func :source-func
                                         target-func :target-func}]
  (log/info (str "updateVersions | Incoming variables are, keepVersions: " keepVersions
                 " key: " k-id " Value: " v-id " Item ids: " ids " Item type: " id-type
                 " Functions: " id-func ", " source-func " & " target-func))
  (let [nods (vec (sort > (map #(:id %) ids)))]
    (log/debug (str "updateVersions | Reverse ordered list of Item ids: " nods))
    (when-not (nil? keepVersions)
      (case keepVersions
            "All"          (auto-enter-links k-id v-id ids :source-func source-func
                                                           :target-func target-func)
            "AllButOldest" (let [nr-ids (count ids)]
                             (do
                               (when (= 1 nr-ids)
                                 (auto-enter-links k-id v-id ids :source-func source-func
                                                                 :target-func target-func))
                               (id-func {:db-type (db-type) :ids [(last nods)]})
                               (db/inactivate-sources! {:db-type (db-type) :id-type id-type :ids [(last nods)]})
                               (db/inactivate-targets! {:db-type (db-type) :id-type id-type :ids [(last nods)]})
                               (when-not (= 1 nr-ids)
                                 (auto-enter-links k-id v-id ids :source-func source-func
                                                                 :target-func target-func))))
            "Last"         (do
                             (id-func {:db-type (db-type) :ids (rest nods)})
                             (db/inactivate-sources! {:db-type (db-type) :id-type id-type :ids (rest nods)})
                             (db/inactivate-targets! {:db-type (db-type) :id-type id-type :ids (rest nods)})
                             (auto-enter-links k-id v-id ids :source-func source-func
                                                             :target-func target-func))
            "None"         (do
                             (auto-enter-links k-id v-id [(last ids)] :source-func source-func
                                                                      :target-func target-func)
                             (id-func {:db-type (db-type) :ids nods})
                             (db/inactivate-sources! {:db-type (db-type) :id-type id-type :ids nods})
                             (db/inactivate-targets! {:db-type (db-type) :id-type id-type :ids nods}))))))


;; Nodes
(defn add-node!
  "Adds new node to an environment

   Aldonas novan nodon al medio"
  [env-name node keepVersions]
  (log/info (str "add-node! | Name: " (:name node) " to be added to environment: " env-name " with keepVersions=" keepVersions))
  (log/info (str "add-node! | Node information: " node))
  (let [env-id (db-check/exist_env env-name)
        nod-ids (db/get-active-nodes {:env_id env-id :name (:name node)})]
    (if (nil? env-id)
        {:result "Environment must exists before adding a node"}
        (try
          (let [ret-val (db/create-node! (merge {:env_id env-id} node))
                nod-id  (case (db-type)
                           :h2        (:id (first ret-val))
                           :sqlserver (int (:generated_keys (first ret-val))))]
             (log/info (str "add-node! | Node with name: " (:name node) " created with ID: " nod-id " in environment: " env-name))
             (when-not (empty? nod-ids)
               (log/info (str "add-node! | Found active nodes with id's: " nod-ids))
               (updateVersions keepVersions :nod_id nod-id nod-ids :node
                               :id-func #(db/inactivate-nodes! %)
                               :source-func #(db/get-active-sources-for-node %)
                               :target-func #(db/get-active-targets-for-node %)))
             nod-id)
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
        (log/info "ret-nodes | Get the nodes for environment: " env-name)
        (db/get-nodes params)))))

;; SubNodes
(defn add-subnode!
  "Adds new subnode to a node

   Aldonas nuvon subnodon al nodo"
  [env-name nod-name nod-version subnode keepVersions]
  (log/info (str "add-subnode! | Name: " (:name subnode) " to be added to environment: " env-name))
  (log/info (str "add-subnode! | Node information: " subnode))
  (let [env-id (db-check/exist_env env-name)
        nod-id (db-check/exist_node env-id nod-name nod-version)
        sub-ids (db/get-active-subnodes {:nod_id nod-id :name (:name subnode)})]
    (if (nil? nod-id)
      {:result "Either Environment or Node does not exist"}
      (try
        (let [ret-val (db/create-subnode! (merge {:nod_id nod-id} subnode))
              sub-id  (case (db-type)
                        :h2 (:id (first ret-val))
                        :sqlserver (int (:generated_keys (first ret-val))))]
          (log/info (str "add-subnode! | SubNode with name: " (:name subnode) " created with ID: " sub-id " in environment: " env-name))
          (log/info (str "Sub-ids: " sub-ids))
          (when-not (empty? sub-ids)
            (log/info (str "add-subnode! | Found active subnodes with id's: " sub-ids))
            (updateVersions keepVersions :sub_id sub-id sub-ids :snod
                            :id-func #(db/inactivate-subnodes! %)
                            :source-func #(db/get-active-sources-for-subnode %)
                            :target-func #(db/get-active-targets-for-subnode %)))
          {:result "SubNode succesfully added"})
        (catch Exception e
               (log/error (str "add-subnode! | Error inserting to db: " e))
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
        (log/info "ret-subnodes | Get the nodes for environment: " env-name " and node: " nod-name " - " nod-version)
        (db/get-subnodes params)))))

(defn add-interface!
  "Adds a new interface to an environment in case it does not exist yet and returns its ID

  Aldonas novan interfacon al medio, se ĝi ankoraŭ ne ekzistas kaj redonas sian identigilon"
  [{:keys [env-id name version type deploymentdate comment]}]
  (log/info (str "add-interface! | Add link name: " name " - " version "with type: " type " to environment with id: " env-id))
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
  (log/info (str "add-source! | Add a source to environment: " env-id " & link: " lin-id))
  (let [nod-id  (db-check/exist_node env-id Node Version)
        sub-id  (db-check/exist_subnode nod-id SubNode SubVersion)
        sou-in  (when-not (nil? nod-id)
                  (db/create-source! {:lin_id lin-id
                                      :nod_id nod-id
                                      :sub_id sub-id}))]
    (if (nil? sou-in)
      (do
        (log/info (str "add-source! | Source-node could not be added; check if the link and node do exist"))
        {:result "Source-node could not be added; check if the link and node do exist"})
      (do
        (log/info (str "add-source! | Source: " Node " - " Version " / " SubNode " - " SubVersion "successfully addedto the link"))
        {:result "Source succesfully added to the link"}))))

(defn add-target!
  "Adds new target to a link in the environment

   Aldonas novan celo al ligo en la medio"
  [env-id lin-id {:keys [Node Version SubNode SubVersion]}]
  (log/info (str "add-target! | Add a target to environment: " env-id " & link: " lin-id))
  (let [nod-id  (db-check/exist_node env-id Node Version)
        sub-id  (db-check/exist_subnode nod-id SubNode SubVersion)
        tar-in  (when-not (nil? nod-id)
                  (db/create-target! {:lin_id lin-id
                                      :nod_id nod-id
                                      :sub_id sub-id}))]
    (if (nil? tar-in)
      (do
        (log/info (str "add-target! | Target-node could not be added; check if the link and node do exist"))
        {:result "Target-node could not be added; check if the link and node do exist"})
      (do
        (log/info (str "add-target! | Target: " Node " - " Version " / " SubNode " - " SubVersion "successfully addedto the link"))
        {:result "Target succesfully added to the link"}))))

(defn add-link!
  "Adds new link to an environment

   Aldonas novan ligon al medio"
  [env-name link keepVersions]
  (log/info (str "add-link! | Name: " (:name link) " to be added to environment: " env-name " with keepVersions=" keepVersions))
  (log/info (str "add-link! | Node information: " link))
  (let [env-id (db-check/exist_env env-name)
        lin-ids (db/get-active-links {:env_id env-id :name (:name link)})]
    (if (nil? env-id)
      {:result "Environment must exists before adding a link"}
      (let [lin-id (add-interface! (merge {:env-id env-id} link))]
        (when (int? lin-id)
          (do
            (log/info (str "add-link! | Link with name: " (:name link) " created with ID: " lin-id " in environment: " env-name))
            (when-not (empty? lin-ids)
              (log/info (str "add-link! | Found active links with id's: " lin-ids))
              (updateVersions keepVersions :lin_id lin-id lin-ids :link
                              :id-func #(db/inactivate-links! %)
                              :source-func #(db/get-active-sources-for-link %)
                              :target-func #(db/get-active-targets-for-link %)))
            (let [source (:source link)
                  sou-in (when-not (and (int? lin-id) (nil? source))
                           (do
                             (log/info (str "add-link! | Adding source to link"))
                             (add-source! env-id lin-id source)))
                  target (:target link)
                  tar-in (when-not (and (int? lin-id) (nil? target))
                           (do
                             (log/info (str "add-link! | Adding target to link"))
                             (add-target! env-id lin-id target)))])))
       (if (int? lin-id)
         {:result "The link was succesfully added"}
         {:result "The link already exists, use add source or add target"})))))


(defn prepare-links
  "Convert the db result to the correct map

   Konvertu la db-rezulton al la ĝusta mapo"
  [link]
  {:name (:name link)
   :type (:type link)
   :version (:version link)
   :deploymentdate (:deploymentdate link)
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
  [k-id v-id ids & {source-func :source-func target-func :target-func}]
  (log/info (str "auto-enter-links | Incoming variables are, Key: " k-id " Value: " v-id
                 " Item ids: " ids " Functions: " source-func " & " target-func))
  (let [sources (source-func {:ids (mapv #(:id %) ids)})
        targets (target-func {:ids (mapv #(:id %) ids)})
        u-sours (seq (set (map #(assoc % k-id v-id) sources)))
        u-targs (seq (set (map #(assoc % k-id v-id) targets)))]
    (log/info (str "auto-enter-links | List of active sources: " sources))
    (log/info (str "auto-enter-links | List of active targets: " targets))
    (log/info (str "auto-enter-links | Unique list of active sources: " u-sours))
    (log/info (str "auto-enter-links | Unique list of active targets: " u-targs))
    (try
      (let [added-sources (case (db-type)
                            :h2 (count (map #(db/create-source! %) u-sours))
                            :sqlserver (count (map #(db/create-source! %) u-sours)))
            added-targets (case (db-type)
                            :h2 (count (map #(db/create-target! %) u-targs))
                            :sqlserver (count (map #(db/create-target! %) u-targs)))]
        (log/info (str "auto-enter-links | Added " added-sources " Sources and " added-targets " Targets to link"))
        (:result "Sources and Targets succefully updated"))
      (catch Exception e
             (log/error (str "Error inserting to db: " e))
             {:result (str "Error inserting to db: " e)}))))
