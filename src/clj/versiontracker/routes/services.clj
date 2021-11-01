(ns versiontracker.routes.services
  (:require
    [reitit.swagger :as swagger]
    [reitit.swagger-ui :as swagger-ui]
    [reitit.ring.coercion :as coercion]
    [reitit.coercion.spec :as spec-coercion]
    [reitit.ring.middleware.muuntaja :as muuntaja]
    [reitit.ring.middleware.multipart :as multipart]
    [reitit.ring.middleware.parameters :as parameters]
    [versiontracker.middleware.formats :as formats]
    [versiontracker.db.data :as vt-data]
    [versiontracker.validation :as vt-vali]
    [versiontracker.export.pdf :as vt-expo]
    [ring.util.http-response :refer :all]
    [clojure.java.io :as io]
    [clojure.spec.alpha :as s]))

(defn service-routes []
  ["/api"
   {:coercion spec-coercion/coercion
    :muuntaja formats/instance
    :swagger {:id ::api}
    :middleware [;; query-params & form-params
                 parameters/parameters-middleware
                 ;; content-negotiation
                 muuntaja/format-negotiate-middleware
                 ;; encoding response body
                 muuntaja/format-response-middleware
                 ;; exception handling
                 coercion/coerce-exceptions-middleware
                 ;; decoding request body
                 muuntaja/format-request-middleware
                 ;; coercing response bodys
                 coercion/coerce-response-middleware
                 ;; coercing request parameters
                 coercion/coerce-request-middleware
                 ;; multipart
                 multipart/multipart-middleware]}

   ;; swagger documentation
   ["" {:no-doc true
        :swagger {:info {:title "Version Tracker"
                         :description "https://cljdoc.org/d/metosin/reitit"}}}

    ["/swagger.json"
     {:get (swagger/create-swagger-handler)}]

    ["/api-docs/*"
     {:get (swagger-ui/create-swagger-ui-handler
             {:url "/api/swagger.json"
              :config {:validator-url nil}})}]]

   ["/environments"
    {:swagger {:tags ["Environments"]}}
    [""
      {:post {:summary "Add a new environment"
              :parameters {:body ::vt-vali/environment}
              :responses {201 {:body map?}}
              :handler (fn [{{:keys [body]} :parameters}]
                        (let [result (vt-data/add-environment! body)]
                          (if (s/valid? int? result)
                            {:status 201
                             :body {:result (str (:name body) " is added")}}
                            {:status 500
                             :body result})))}
       :get {:summary "Get the list of environments"
             :responses {200 {:body ::vt-vali/environments}}
             :handler (fn [_]
                        (let [result (vt-data/ret-environments)]
                          (if (s/valid? ::vt-vali/environments result)
                            {:status 200
                             :body result}
                            {:status 500
                             :body result})))}}]


    ["/:env-name"
     {:get {:summary "Get the information for an environment"
            :parameters {:path {:env-name ::vt-vali/name}}
            :responses {200 {:body ::vt-vali/environment}}
            :handler (fn [{{{:keys [env-name]} :path} :parameters}]
                       (let [result (vt-data/ret-environment env-name)]
                         (if (s/valid? ::vt-vali/environment result)
                          {:status 200
                           :body result}
                          {:status 500
                           :body result})))}}]]

   ["/environments/:env-name/nodes"
    {:swagger {:tags ["Nodes"]}
     :parameters {:path {:env-name ::vt-vali/name}}}
    [""
     {:post {:summary "Add a new node to an environment"
             :description "Default value for keepVersions is None. This means by default only the last version is kept active."
             :parameters {:query (s/keys :opt-un [::vt-vali/keepVersions])
                          :body ::vt-vali/node}
             :responses {201 {:body map?}}
             :handler (fn [{{{:keys [env-name]} :path
                             {:keys [keepVersions]} :query
                             :keys [body]} :parameters}]
                        (let [result (vt-data/add-node! env-name body keepVersions)]
                          (if (s/valid? int? result)
                            {:status 201
                             :body {:result "Node succesfully added"}}
                            {:status 500
                             :body result})))}
      :get {:summary "Retrieve the nodes from this environment"
            :parameters {:query (s/keys :opt-un [::vt-vali/date])}
            :responses {200 {:body ::vt-vali/nodes}
                        500 {:body string?}}
            :handler (fn [{{{:keys [env-name]} :path
                            {:keys [date]} :query} :parameters}]
                       (let [result (vt-data/ret-nodes env-name date)]
                         (if (s/valid? ::vt-vali/nodes result)
                           {:status 200
                            :body result}
                           {:status 500
                            :body (s/explain ::vt-vali/nodes result)})))}}]]

   ["/environments/:env-name/nodes/:nod-name/subnodes"
     {:swagger {:tags ["SubNodes"]}
      :parameters {:path {:env-name ::vt-vali/name
                          :nod-name ::vt-vali/name}}}
    [""
     {:post {:summary "Add a new subnode to a node"
             :parameters {:query (s/keys :req-un [::vt-vali/nod-version]
                                         :opt-un [::vt-vali/keepVersions])
                          :body ::vt-vali/subnode}
             :responses {201 {:body map?}}
             :handler (fn [{{{:keys [env-name nod-name]} :path
                             {:keys [nod-version keepVersions]} :query
                             :keys [body]} :parameters}]
                        (let [result (vt-data/add-subnode! env-name nod-name nod-version body keepVersions)]
                          (if (s/valid? map? result)
                            {:status 201
                             :body result}
                            {:status 500
                             :body result})))}
      :get {:summary "Retrieve the subnodes from a node"
            :parameters {:query (s/keys :req-un [::vt-vali/nod-version]
                                        :opt-un [::vt-vali/date])}
            :responses {200 {:body ::vt-vali/subnodes}}
            :handler (fn [{{{:keys [env-name nod-name]} :path
                            {:keys [nod-version date]} :query} :parameters}]
                       (let [result (vt-data/ret-subnodes env-name nod-name nod-version date)]
                         (if (s/valid? ::vt-vali/subnodes result)
                           {:status 200
                            :body result}
                           {:status 500
                            :body result})))}}]]

   ["/environments/:env-name/links"
    {:swagger {:tags ["Links"]}
     :parameters {:path {:env-name ::vt-vali/name}}}
    [""
     {:post {:summary "Add a new link"
             :parameters {:query (s/keys :opt-un [::vt-vali/keepVersions])
                          :body ::vt-vali/link}
             :responses {201 {:body map?}}
             :handler (fn [{{{:keys [env-name]} :path
                             {:keys [keepVersions]} :query
                             :keys [body]} :parameters}]
                        (let [result (vt-data/add-link! env-name body keepVersions)]
                          (if (s/valid? map? result)
                            {:status 201
                             :body result}
                            {:status 500
                             :body result})))}
      :get {:summary "Retrieve the links from an environment"
            :parameters {:query (s/keys :opt-un [::vt-vali/date])}
            :responses {200 {:body ::vt-vali/links}}
            :handler (fn [{{{:keys [env-name]} :path
                            {:keys [date]} :query} :parameters}]
                       (let [result (vt-data/ret-links env-name date)]
                         (if (s/valid? ::vt-vali/links result)
                           {:status 200
                            :body result}
                           {:status 500
                            :body result})))}}]
    ["/pdf"
      {:get {:summary "Retrieve links as PDF"
             :parameters {:query (s/keys :opt-un [::vt-vali/date])}
             :response {200 {:body (java.io.ByteArrayOutputStream.)}}
             :headers {"Content-Type" "application/pdf"}
             :handler (fn [{{{:keys [env-name]} :path
                             {:keys [date]} :query} :parameters}]
                         {:status 200
                          :body (vt-expo/create-pdf env-name date)})}}]


    ["/:link-name"
     {:parameters {:path {:link-name ::vt-vali/name}
                   :query {:link-version ::vt-vali/version}}}
     ["/source"
      {:post {:summary "Add a source"
              :parameters {:body ::vt-vali/source}
              :responses {201 {:body map?}}
              :handler (fn [{{{:keys [env-name link-name]} :path
                              {:keys [link-version]} :query
                              :keys [body]} :parameters}]
                         (let [result (vt-data/add-node-to-link! :source env-name link-name link-version body)]
                           (if (s/valid? map? result)
                             {:status 201
                              :body result}
                             {:status 500
                              :body result})))}}]
     ["/target"
      {:post {:summary "Add a target"
              :parameters {:body ::vt-vali/target}
              :responses {201 {:body map?}}
              :handler (fn [{{{:keys [env-name link-name]} :path
                              {:keys [link-version]} :query
                              :keys [body]} :parameters}]
                         (let [result (vt-data/add-node-to-link! :target env-name link-name link-version body)]
                           (if (s/valid? map? result)
                             {:status 201
                              :body result}
                             {:status 500
                              :body result})))}}]]]])
