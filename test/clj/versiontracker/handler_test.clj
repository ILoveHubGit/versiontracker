(ns versiontracker.handler-test
  (:require
    [clojure.test :refer :all]
    [ring.mock.request :refer :all]
    [versiontracker.handler :refer :all]
    [versiontracker.middleware.formats :as formats]
    [versiontracker.validation :as vt-vali]
    [muuntaja.core :as m]
    [mount.core :as mount]
    [clojure.spec.alpha :as s]))

(defn parse-json [body]
  (m/decode formats/instance "application/json" body))

(use-fixtures
  :once
  (fn [f]
    (mount/start #'versiontracker.config/env
                 #'versiontracker.handler/app-routes)
    (f)))

(deftest test-app
  (testing "main route"
    (let [response ((app) (request :get "/"))]
      (is (= 200 (:status response)))))

  (testing "not-found route"
    (let [response ((app) (request :get "/invalid"))]
      (is (= 404 (:status response)))))


  (testing "services"

    (testing "success-get-environments"
      (let [response ((app) (-> (request :get "/api/environments")))]
        (is (= 200 (:status response)))
        (is (s/valid? ::vt-vali/environments (m/decode-response-body response)))))


    (testing "success-get-environment"
      (let [response ((app) (-> (request :get "/api/environments/TestEnvironment")))]
        (is (= 200 (:status response)))
        (is (s/valid? ::vt-vali/environment (m/decode-response-body response)))))

    (testing "success-get-nodes"
      (let [response ((app) (-> (request :get "/api/environments/TestEnvironment/nodes")))]
        (is (= 200 (:status response)))
        (is (s/valid? ::vt-vali/nodes (m/decode-response-body response)))))

    (testing "success-get-subnodes"
      (let [response ((app) (-> (request :get "/api/environments/TestEnvironment/nodes/Node-1/subnodes")))]
        (is (= 400 (:status response)))))

    (testing "success-get-links"
      (let [response ((app) (-> (request :get "/api/environments/TestEnvironment/links")))]
        (is (= 200 (:status response)))
        (is (s/valid? ::vt-vali/links (m/decode-response-body response)))))))
