(ns versiontracker.graph
  (:require [clojure.string :as c-str]))

(declare convert-links)

(defn graph-view
  [links select-view]
  (let [new-links (convert-links links)]
    [:div {:hidden @select-view}
      [:div {:id "#graph-view"}
       [:div "Links: " (str links)]
       [:br]
       [:div "New-Links: " (str new-links)]]]))


(defn add-nodes
  [link]
  (let [source (:source link)
        target (:target link)]
    [{:id (str "I" (:linid link))
      :name (:name link)
      :version (:version link)
      :type :link}
     (when (:Node source)
      {:id (str "N" (:id source))
       :name (:Node source)
       :version (:Version source)
       :type :node})
     (when (:SubNode source)
      {:id (str "S" (:subid source))
       :name (:SubNode source)
       :version (:SubVersion source)
       :type :subnode})
     (when (:Node target)
      {:id (str "N" (:id target))
       :name (:Node target)
       :version (:Version target)
       :type :node})
     (when (:SubNode target)
      {:id (str "S" (:subid target))
       :name (:SubNode target)
       :version (:SubVersion target)
       :type :subnode})]))

(defn add-ns-link
  [side node]
  (when-not (or (nil? (:Node (side node))) (nil? (:SubNode (side node))))
    {:source (str "N" (:id (side node)))
     :target (str "S" (:subid (side node)))
     :type "ns"
     :name "function"
     :version "N/A"}))

(defn add-in-link
  [link]
  (let [source (:source link)
        s-node (if (nil? (:SubNode source))
                 (str "N" (:id source))
                 (str "S" (:subid source)))
        target (:target link)
        t-node (if (nil? (:SubNode target))
                 (str "N" (:id target))
                 (str "S" (:subid target)))]
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
        (flatten (mapv #(add-nodes %) links))))
    :links
    (filterv #(not (nil? %))
      (set
        (concat
          (mapv #(add-ns-link :source %) links)
          (mapv #(add-ns-link :target %) links)
          (mapv #(add-in-link %) links))))})
