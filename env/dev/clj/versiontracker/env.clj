(ns versiontracker.env
  (:require
    [selmer.parser :as parser]
    [clojure.tools.logging :as log]
    [versiontracker.dev-middleware :refer [wrap-dev]]))

(def defaults
  {:init
   (fn []
     (parser/cache-off!)
     (log/info "\n-=[versiontracker started successfully using the development profile]=-"))
   :stop
   (fn []
     (log/info "\n-=[versiontracker has shut down successfully]=-"))
   :middleware wrap-dev})
