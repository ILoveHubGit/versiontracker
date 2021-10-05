(ns versiontracker.db.core-test
  (:require
   [versiontracker.db.core :refer [*db*] :as db]
   [java-time.pre-java8]
   [luminus-migrations.core :as migrations]
   [clojure.test :refer :all]
   [next.jdbc :as jdbc]
   [versiontracker.config :refer [env]]
   [versiontracker.validation :as vt-vali]
   [mount.core :as mount]
   [clojure.spec.alpha :as s]))

(use-fixtures
  :once
  (fn [f]
    (mount/start
     #'versiontracker.config/env
     #'versiontracker.db.core/*db*)
    (migrations/migrate ["migrate"] (select-keys env [:database-url]))
    (f)))

(deftest test-add-environment
  (jdbc/with-transaction [t-conn *db* {:rollback-only true}]
    (is (number? (:id (first (db/create-environment!
                              t-conn
                              {:name "TestEnvironment"
                               :comment "No comment"}
                              {})))))
    (is (= {:name "TestEnvironment"
            :comment "No comment"}
           (db/get-environment t-conn {:env_name "TestEnvironment"} {})))))

(deftest test-get-environments
  (jdbc/with-transaction [t-conn *db*]
    (is (s/valid? ::vt-vali/environments
                  (db/get-environments t-conn {} {})))))

(deftest test-add-link
  (jdbc/with-transaction [t-conn *db* {:rollback-only true}]
    (let [link-id (:id (first (db/create-link!
                                t-conn
                                {:env_id 1
                                 :name "TestLink"
                                 :type "API"
                                 :version "1.0"
                                 :deploymentdate "2021-10-04T23:36:00"
                                 :comment "No comment"}
                                {})))]
      (is (number? link-id))
      (is (= {:id link-id} (db/get-link-id t-conn {:name "TestLink" :version "1.0"}))))))
