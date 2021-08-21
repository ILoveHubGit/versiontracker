(ns versiontracker.env
  (:require [clojure.tools.logging :as log]))

(def defaults
  {:init
   (fn []
     (log/info "\n-=[versiontracker started successfully]=-"))
   :stop
   (fn []
     (log/info "\n-=[versiontracker has shut down successfully]=-"))
   :middleware identity})
