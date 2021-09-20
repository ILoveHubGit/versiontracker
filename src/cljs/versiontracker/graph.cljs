(ns versiontracker.graph
  (:require [clojure.string :as c-str]))

(declare convert-links)

(defn graph-view
  [links select-view]
  (let [new-links (convert-links links)]
    [:div {:hidden @select-view}
      [:div {:id "#graph-view"} (str new-links)]]))

(defn add-node
  [node sub?]
  (let [k-node (if sub? :SubNode :Node)
        k-version (if sub? :SubVersion :Version)
        k-id (if sub? (str "S" (:subId node)) (str "N" (:id node)))]
    (when-not (nil? (k-node node))
      {:id k-id
       :name (k-node node)
       :version (k-version node)})))

(defn add-ns-link
  [node]
  (when-not (or (nil? (:Node node)) (nil? (:SubNode node)))
    {:source (str "N" (:id node))
     :target (str "S" (:subId node))
     :type "ns"
     :name "function"
     :version "N/A"}))

(defn add-in-link
  [link]
  (let [source (:source link)
        s-node (if (nil? (:SubNode source))
                 (str "N" (:id source))
                 (str "S" (:subId source)))
        target (:target link)
        t-node (if (nil? (:SubNode target))
                 (str "N" (:id target))
                 (str "S" (:subId target)))]
    (when-not (and (nil? (:Node source))
                   (nil? (:Node target)))
      {:source s-node
       :target t-node
       :type (:type link)
       :name (:name link)
       :version (:version link)})))

(defn convert-links
  [links]
  {:nodes
    (filterv #(not (nil? %))
      (set
        (concat
          (mapv #(add-node (:source %) false) links)
          (mapv #(add-node (:target %) false) links)
          (mapv #(add-node (:source %) true) links)
          (mapv #(add-node (:target %) true) links))))
    :links
    (filterv #(not (nil? %))
      (set
        (concat
          (mapv #(add-ns-link (:source %)) links)
          (mapv #(add-ns-link (:target %)) links)
          (mapv #(add-in-link %) links))))})
