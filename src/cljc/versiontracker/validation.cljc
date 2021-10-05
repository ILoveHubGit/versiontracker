(ns versiontracker.validation
  (:require [clojure.spec.alpha :as s]))

(s/def ::name string?)
(s/def ::comment (s/nilable string?))
(s/def ::type (s/nilable string?))
(s/def ::version string?)
(s/def ::deploymentdate string?)
; (s/def ::deploymentdate instance?)
(s/def ::Node (s/nilable ::name))
(s/def ::Version (s/nilable ::version))
(s/def ::SubNode (s/nilable ::name))
(s/def ::SubVersion (s/nilable ::version))
(s/def ::date inst?)
(s/def ::nod-name ::name)
(s/def ::nod-version ::version)


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
                               ::source
                               ::target]))
(s/def ::links (s/coll-of ::link))
