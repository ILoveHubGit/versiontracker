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
        (is (s/valid? ::vt-vali/environments (m/decode-response-body response)))))))

    ; (testing "success-post-environment"
    ;   (let [response ((app) (-> (request :post "/api/environments")
    ;                             (json-body {:name "TestEnvironment" :comment "No comment"})))]
    ;     (is (= 200 (:status response)))
    ;     (is (= "TestEnvironment is added" (:result (m/decode-response-body response))))))
    ;
    ; (testing "success-get-environment"
    ;   (let [response ((app) (-> (request :get "/api/environments/TestEnvironment")))]
    ;     (is (= 200 (:status response)))
    ;     (is (s/valid? ::vt-vali/environment (m/decode-response-body response)))))))

    ; (testing "success-get-nodes"
    ;   (let [response ((app) (-> (request :get "/api/environments/myTest/nodes")))]
    ;     (is (= 200 (:status response)))
    ;     (is (s/valid? ::vt-vali/nodes (m/decode-response-body response)))))
    ;
    ; (testing "success-get-subnodes"
    ;   (let [response ((app) (-> (request :get "/api/environments/myTest/nodes/Node-1/subnodes")))]
    ;     (is (= 200 (:status response)))
    ;     (is (s/valid? ::vt-vali/subnodes (m/decode-response-body response)))))
    ;
    ; (testing "success-get-links"
    ;   (let [response ((app) (-> (request :get "/api/environments/myTest/links")))]
    ;     (is (= 200 (:status response)))
    ;     (is (s/valid? ::vt-vali/links (m/decode-response-body response)))))))
    ; (testing "success"
    ;   (let [response ((app) (-> (request :post "/api/math/plus")
    ;                             (json-body {:x 10, :y 6})))]
    ;     (is (= 200 (:status response)))
    ;     (is (= {:total 16} (m/decode-response-body response)))))
    ;
    ; (testing "parameter coercion error"
    ;   (let [response ((app) (-> (request :post "/api/math/plus")
    ;                             (json-body {:x 10, :y "invalid"})))]
    ;     (is (= 400 (:status response)))))
    ;
    ; (testing "response coercion error"
    ;   (let [response ((app) (-> (request :post "/api/math/plus")
    ;                             (json-body {:x -10, :y 6})))]
    ;     (is (= 500 (:status response)))))
    ;
    ; (testing "content negotiation"
    ;   (let [response ((app) (-> (request :post "/api/math/plus")
    ;                             (body (pr-str {:x 10, :y 6}))
    ;                             (content-type "application/edn")
    ;                             (header "accept" "application/transit+json")))]
    ;     (is (= 200 (:status response)))
    ;     (is (= {:total 16} (m/decode-response-body response)))))))
