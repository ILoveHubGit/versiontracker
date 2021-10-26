(ns versiontracker.db.core-test
  (:require [versiontracker.db.core :refer [*db*] :as db]
            [java-time.pre-java8]
            [luminus-migrations.core :as migrations]
            [clojure.test :refer :all]
            [next.jdbc :as jdbc]
            [versiontracker.config :refer [env]]
            [versiontracker.validation :as vt-vali]
            [mount.core :as mount]
            [clojure.spec.alpha :as s]
            [clojure.string :as cstr]))


(defn db-type [] (keyword (second (cstr/split (env :database-url) #":"))))

(use-fixtures
  :once
  (fn [f]
    (mount/start
     #'versiontracker.config/env
     #'versiontracker.db.core/*db*)
    (migrations/migrate ["migrate"] (select-keys env [:database-url :migration-dir]))
    (f)))

(deftest test-environment
  (jdbc/with-transaction [t-conn *db* {:rollback-only true}]
    (let [environment (db/create-environment!
                        t-conn
                        {:name "NewEnvironment"
                         :comment "No comment"}
                        {})]
      (is (number? (case (db-type)
                         :h2 (:id (first environment))
                         :sqlserver (int (:generated_keys (first environment))))))
      (is (= {:name "NewEnvironment"
              :comment "No comment"}
             (db/get-environment t-conn {:env_name "NewEnvironment"} {}))))))


(deftest test-node
  (jdbc/with-transaction [t-conn *db* {:rollback-only true}]
    (let [dbtype      (db-type)
          environment (db/create-environment!
                        t-conn
                        {:name "NodeEnvironment"
                         :comment "No comment"}
                        {})
          env-id      (case dbtype
                            :h2 (:id (first environment))
                            :sqlserver (int (:generated_keys (first environment))))
          node        (db/create-node!
                        t-conn
                        {:env_id env-id
                         :name "TestNode"
                         :type "Application"
                         :version "Test1"
                         :deploymentdate "2021-10-12 19:49:00"
                         :comment "No comment"}
                        {})
          nod-id       (case dbtype
                             :h2 (:id (first node))
                             :sqlserver (int (:generated_keys (first node))))]
      (is (number? nod-id))
      (is (s/valid? ::vt-vali/nodes
             (db/get-nodes t-conn {:db-type dbtype :env_id env-id} {}))))))

(deftest test-subnode
  (jdbc/with-transaction [t-conn *db* {:rollback-only true}]
    (let [dbtype      (db-type)
          environment (db/create-environment!
                        t-conn
                        {:name "SubnodeEnvironment"
                         :comment "No comment"}
                        {})
          env-id      (case dbtype
                            :h2 (:id (first environment))
                            :sqlserver (int (:generated_keys (first environment))))
          node        (db/create-node!
                        t-conn
                        {:env_id env-id
                         :name "SubNode"
                         :type "Application"
                         :version "Test1"
                         :deploymentdate "2021-10-12 19:49:00"
                         :comment "No comment"}
                        {})
          nod-id       (case dbtype
                             :h2 (:id (first node))
                             :sqlserver (int (:generated_keys (first node))))
          subnode      (db/create-subnode!
                         t-conn
                         {:nod_id nod-id
                          :name "TestSubNode"
                          :version "Sub1"
                          :deploymentdate "2021-10-12 19:49:00"
                          :comment "Testing"})
          sub-id       (case dbtype
                             :h2 (:id (first subnode))
                             :sqlserver (int (:generated_keys (first subnode))))]
      (is (number? sub-id))
      (is (s/valid? ::vt-vali/subnodes
             (db/get-subnodes t-conn {:db-type dbtype :nod_id nod-id} {}))))))

(deftest test-link
  (jdbc/with-transaction [t-conn *db* {:rollback-only true}]
    (let [dbtype      (db-type)
          environment (db/create-environment!
                        t-conn
                        {:name "LinkEnvironment"
                         :comment "No comment"}
                        {})
          env-id      (case dbtype
                            :h2 (:id (first environment))
                            :sqlserver (int (:generated_keys (first environment))))
          link        (db/create-link!
                        t-conn
                        {:env_id env-id
                         :name "TestLink"
                         :type "API"
                         :version "1.0"
                         :deploymentdate "2021-10-04T23:36:00"
                         :comment "No comment"}
                        {})
          link-id     (case dbtype
                            :h2 (:id (first link))
                            :sqlserver (int (:generated_keys (first link))))]
      (is (number? link-id))
      (is (= {:id link-id} (db/get-link-id t-conn {:env_id env-id :name "TestLink" :version "1.0"}))))))
