(ns versiontracker.validation
  (:require [clojure.spec.alpha :as s]
            [spec-tools.core :as st]
            [clojure.string :as cstr]))

(s/def ::name
       (st/spec
         {:spec string?
          :description "Name of the item"}))
(s/def ::comment
       (st/spec
         {:spec (s/nilable string?)
          :description "Any comment you want to add to the item"}))
(s/def ::type
       (st/spec
         {:spec (s/nilable string?)
          :description "For Nodes and SubNodes any string is right. For Links it is recommended to use one of these:api database webservice file stream queue realtime real-time (case insensitive)"}))
(s/def ::version
       (st/spec
        {:spec string?
         :description "String that idenifies the version of the item"}))
(s/def ::deploymentdate
       (st/spec
         {:spec (s/nilable string?)
          :description "Deploymentdate in one of these formats \"yyyy-mm-dd\", \"yyyy-mm-dd HH:MM:ss\" or \"yyyy-mm-dd HH:MM:ssZ\""}))
(s/def ::Node
       (st/spec
        {:spec (s/nilable ::name)
         :description "The name of an existing Node. If the Node does noet exist it will be added."}))
(s/def ::Version (s/nilable ::version))
(s/def ::SubNode
        (st/spec
          {:spec (s/nilable ::name)
           :description "The name of an existing SubNode. If the SubNode does noet exist it will be added."}))
(s/def ::SubVersion (s/nilable ::version))
(s/def ::date
       (st/spec
         {:spec string?
          :description "Date in one of these formats \"yyyy-mm-dd\", \"yyyy-mm-dd HH:MM:ss\" or \"yyyy-mm-dd HH:MM:ssZ\""}))
(s/def ::nod-name ::name)
(s/def ::nod-version ::version)
(s/def ::keepVersions
       (st/spec
         {:spec #{"None" "All" "Last" "AllButOldest"}
          :description "Use to define how many versions of the items have to be kept active"
          :swagger/default "None"}))

;; Groups
(s/def ::environment (s/keys :req-un [::name]
                             :opt-un [::comment]))
(s/def ::environments (s/coll-of ::environment))

(s/def ::subnode (s/keys :req-un [::name ::version]
                         :opt-un [::deploymentdate ::comment]))
(s/def ::subnodes (s/coll-of ::subnode))

(s/def ::node (s/keys :req-un [::name ::version]
                      :opt-un [::type ::deploymentdate ::comment]))
(s/def ::nodes (s/coll-of ::node))

(s/def ::source (s/keys :req-un [::Node ::Version]
                        :opt-un [::SubNode ::SubVersion]))
(s/def ::target ::source)

(s/def ::link (s/keys :req-un [::name ::type ::version]
                      :opt-un [::deploymentdate ::comment
                               ::source ::target]))
(s/def ::links (s/coll-of ::link))
