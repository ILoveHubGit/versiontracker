(defproject versiontracker "0.2.1"

  :description "FIXME: write description"
  :url "http://example.com/FIXME"

  :dependencies [[buddy/buddy-auth "3.0.1"]
                 [buddy/buddy-core "1.10.1"]
                 [buddy/buddy-hashers "1.8.1"]
                 [buddy/buddy-sign "3.4.1"]
                 [ch.qos.logback/logback-classic "1.2.6"]
                 [clj-http "3.12.3"]
                 [clj-pdf "2.5.8"]
                 [cljs-ajax "0.8.4"]
                 ; [cljsjs/d3 "4.12.0-0"]
                 [clojure-interop/java.io "1.0.5"]
                 [clojure.java-time "0.3.3"]
                 [com.cognitect/transit-clj "1.0.324"]
                 [com.cognitect/transit-cljs "0.8.269"]
                 [com.h2database/h2 "1.4.200"]
                 [com.microsoft.sqlserver/sqljdbc4 "4.0"]
                 [conman "0.9.1"]
                 [cprop "0.1.19"]
                 [day8.re-frame/http-fx "0.2.3"]
                 [expound "0.8.10"]
                 [funcool/struct "1.4.0"]
                 [json-html "0.4.7"]
                 [kee-frame "1.3.1" :exclusions [metosin/reitit-core org.clojure/core.async]]
                 [lock-key "1.5.0"]
                 [luminus-migrations "0.7.1"]
                 [luminus-transit "0.1.2"]
                 [luminus-undertow "0.1.12"]
                 [luminus/ring-ttl-session "0.3.3"]
                 [markdown-clj "1.10.6"]
                 [metosin/muuntaja "0.6.8"]
                 [metosin/reitit "0.5.15"]
                 [metosin/ring-http-response "0.9.3"]
                 [metosin/spec-tools "0.10.5"]
                 [mount "0.1.16"]
                 [mysql/mysql-connector-java "8.0.27"]
                 [nrepl "0.8.3"]
                 [org.clojure/clojure "1.10.3"]
                 [org.clojure/clojurescript "1.10.891" :scope "provided"]
                 [org.clojure/core.async "1.4.627"]
                 [org.clojure/data.json "2.4.0"]
                 [org.clojure/tools.cli "1.0.206"]
                 [org.clojure/tools.logging "1.1.0"]
                 [org.webjars.npm/bulma "0.9.3"]
                 [org.webjars.npm/material-icons "1.7.1"]
                 [org.webjars/webjars-locator "0.42"]
                 [org.webjars/webjars-locator-jboss-vfs "0.1.0"]
                 [re-frame "1.2.0"]
                 [rid3 "0.2.2"]
                 [reagent "1.1.0"]
                 [ring-webjars "0.2.0"]
                 [ring/ring-core "1.9.4"]
                 [ring/ring-defaults "0.3.3"]
                 [selmer "1.12.44"]
                 [thheller/shadow-cljs "2.15.13" :scope "provided"]]

  :min-lein-version "2.0.0"

  :source-paths ["src/clj" "src/cljs" "src/cljc"]
  :test-paths ["test/clj"]
  :resource-paths ["resources" "target/cljsbuild"]
  :target-path "target/%s/"
  :main ^:skip-aot versiontracker.core

  :plugins [[lein-kibit "0.1.2"]
            [lein-shadow "0.4.0"]
            [lein-codox "0.10.7"]]
  :codox {:output-path "docs"
          :source-uri "https://github.com/ILoveHubGit/versiontracker/tree/main/{filepath}#L{line}"}
  :clean-targets ^{:protect false}
  [:target-path "target/cljsbuild"]
  :shadow-cljs
  {:nrepl {:port 7002}
   :builds
   {:app
    {:target :browser
     :output-dir "target/cljsbuild/public/js"
     :asset-path "/js"
     :modules {:app {:entries [versiontracker.app]}}
     :devtools
     {:watch-dir "resources/public" :preloads [re-frisk.preload]}
     :dev
     {:closure-defines {"re_frame.trace.trace_enabled_QMARK_" true}}}
    :test
    {:target :node-test
     :output-to "target/test/test.js"
     :autorun true}}}

  :npm-deps [[shadow-cljs "2.8.69"]
             [create-react-class "15.6.3"]
             [react "16.9.0"]
             [react-dom "16.9.0"]]
  :npm-dev-deps [[xmlhttprequest "1.8.0"]]

  :profiles
  {:uberjar {:omit-source true
             :prep-tasks ["compile" ["run" "-m" "shadow.cljs.devtools.cli" "release" "app"]]
             :aot :all
             :uberjar-name "versiontracker.jar"
             :source-paths ["env/prod/clj"  "env/prod/cljs"]
             :resource-paths ["env/prod/resources"]}

   :dev           [:project/dev :profiles/dev]
   :test          [:project/dev :project/test :profiles/test]

   :project/dev  {:jvm-opts ["-Dconf=dev-config.edn"]
                  :dependencies [[binaryage/devtools "1.0.4"]
                                 [cider/piggieback "0.5.3"]
                                 [pjstadig/humane-test-output "0.11.0"]
                                 [prone "2021-04-23"]
                                 [re-frisk "1.5.1"]
                                 [ring/ring-devel "1.9.4"]
                                 [ring/ring-mock "0.4.0"]]
                  :plugins      [[com.jakemccrary/lein-test-refresh "0.24.1"]
                                 [jonase/eastwood "0.3.5"]
                                 [cider/cider-nrepl "0.26.0"]]


                  :source-paths ["env/dev/clj"  "env/dev/cljs" "test/cljs"]
                  :resource-paths ["env/dev/resources"]
                  :repl-options {:init-ns user
                                 :timeout 120000}
                  :injections [(require 'pjstadig.humane-test-output)
                               (pjstadig.humane-test-output/activate!)]}
   :project/test {:jvm-opts ["-Dconf=test-config.edn"]
                  :resource-paths ["env/test/resources"]}



   :profiles/dev {}
   :profiles/test {}})
