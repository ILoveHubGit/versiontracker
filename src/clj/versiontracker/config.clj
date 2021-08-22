(ns versiontracker.config
  (:require
    [cprop.core :refer [load-config]]
    [cprop.source :as source]
    [clojure.tools.logging :as log]
    [mount.core :refer [args defstate]]
    [lock-key.core :refer [decrypt decrypt-as-str decrypt-from-base64
                           encrypt encrypt-as-base64]]))

(defstate env
  :start
  (load-config
    :merge
    [(args)
     (source/from-system-props)
     (source/from-env)]))

(defn demo-file
  "Read the config file"
  []
  (read-string (slurp "example/demo-environment.edn")))

;; Functies to encrypt-decrypt the config file
; (def lock "ClojureRules")
; (def file "vt-config.edn")

; (defn get-configuration
;   []
;   (let [result (try
;                  (read-string (decrypt-from-base64 (slurp file) lock))
;                  (catch Exception e
;                         (log/error (str " Can't find file: " file " Error: " (.getMessage e)))))]
;     (if (or (nil? result) (empty? result))
;       {:result "Can't find a configuration file"}
;       result)))
;
;
; (defn set-configuration
;   [content]
;   (let [result (try
;                  (spit file (encrypt-as-base64 (str content) lock))
;                  (catch Exception e
;                         (log/error (str "Can't write file: " file " Error: " (.getMessage e)))))]
;     (if (nil? result)
;       {:result "Configuration file succesfully updated."}
;       {:result result})))
;
; (defn check-configuration
;   "Encrypt the config file if needed"
;   []
;   (when (= \{ (first (slurp file)))
;     (set-configuration (read-string (slurp file)))))

; (check-configuration)
