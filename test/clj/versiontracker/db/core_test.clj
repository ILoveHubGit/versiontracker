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
           (dissoc (db/get-environment t-conn {:env_name "TestEnvironment"} {}) :id)))))

(deftest test-get-environments
  (jdbc/with-transaction [t-conn *db*]
    (is (s/valid? ::vt-vali/environments
                  (db/get-environments t-conn {} {})))))
