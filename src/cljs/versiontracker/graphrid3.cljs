(ns versiontracker.graphrid3
  (:require [rid3.core :as rid3]
            [re-frame.core :as rf]
            [versiontracker.events :as vt-even]
            [versiontracker.subs :as vt-subs]))

(declare sim-did-update)

(enable-console-print!)
(def log (.-log js/console))

(declare convert-links)

(defn remove-svg []
  (-> js/d3
      (.selectAll "#graph svg")
      (.remove)))

(defn get-radius []
  (let [radius (/ @(rf/subscribe [::vt-subs/svg-width]) 45)]
    ; (log "Radius: " radius)
    radius))

(defn force-viz [ratom]
  (log "Force-viz is called")
  (let [radius (get-radius)]
    [rid3/viz
      {:id "force"
       :ratom ratom
       :svg {:did-mount (fn [node ratom]
                          (-> node
                              (.attr "width" (:svg-width @ratom))
                              (.attr "height" (:svg-height @ratom))
                              (.style "background-color" "rgb(245,245,245)")))}
       :pieces [{:kind :elem-with-data
                 :tag "line"
                 :class "lines"
                 :did-mount (fn [node ratom]
                              (let [r (-> node
                                          (.attr "stroke-width" 2))]
                                (rf/dispatch-sync [::vt-even/set-var :link-elems r])))
                 :prepare-dataset (fn [ratom]
                                    (-> @ratom
                                        (get :lines)
                                        clj->js))}
                {:kind :elem-with-data
                 :tag "circle"
                 :class "node"
                 :did-mount (fn [node ratom]
                              (let [r (-> node
                                          (.attr "r" (fn [d] (case (str (first (.-id d)))
                                                                   "I" (* 0.8 radius)
                                                                   "N" (* 1.2 radius)
                                                                   "S" radius)))
                                          (.attr "class" (fn [d] (case (str (first (.-id d)))
                                                                       "I" "Interface"
                                                                       "N" "Node"
                                                                       "S" "SubNode"))))]
                                (rf/dispatch-sync [::vt-even/set-var :node-elems r])))
                 :prepare-dataset (fn [ratom]
                                    (-> @ratom
                                        (get :nodes)
                                        clj->js))}

                {:kind :raw
                 :did-mount sim-did-update
                 :did-update sim-did-update}]}]))

(defn main-panel []
  (let [data (rf/subscribe [::vt-subs/clinks])]
    [force-viz data]))

(defn graph-view
  [select-view]
  [:div {:hidden @select-view}
   (main-panel)])

(defn sim-did-update [ratom]
  (log "Sim is called")
  (let [sim (-> (js/d3.forceSimulation)
                (.force "link" (-> js/d3
                                   (.forceLink)
                                   (.strength 1.5)
                                   (.id (fn [d] (.-id d)))))
                (.force "charge" (-> js/d3
                                     (.forceManyBody)
                                     (.strength -50)))
                (.force "center" (js/d3.forceCenter (/ (:svg-width @ratom) 2)
                                                    (/ (:svg-height @ratom) 2)))
                (.force "collision" (-> js/d3
                                        (.forceCollide)
                                        (.radius (* 1.2 (get-radius))))))
        node-dataset (clj->js (-> @ratom
                                  (get :nodes)))
        link-dataset (clj->js (-> @ratom
                                  (get :lines)))
        node-elems @(rf/subscribe [::vt-subs/get-var :node-elems])
        link-elems @(rf/subscribe [::vt-subs/get-var :link-elems])
        radius (get-radius)
        tick-handler (fn []
                       (-> node-elems
                           (.attr "r" (fn [d]
                                        (case (str (first (.-id d)))
                                              "I" (* 0.8 radius)
                                              "N" (* 1.2 radius)
                                              "S" radius)))
                           (.attr "cx" (fn [_ idx]
                                         (.-x (get node-dataset idx))))
                           (.attr "cy" (fn [_ idx]
                                         (.-y (get node-dataset idx)))))

                       (-> link-elems
                           (.attr "x1" (fn [_ idx]
                                         (-> (get link-dataset idx)
                                             .-source .-x)))
                           (.attr "y1" (fn [_ idx]
                                         (-> (get link-dataset idx)
                                             .-source .-y)))
                           (.attr "x2" (fn [_ idx]
                                         (-> (get link-dataset idx)
                                             .-target .-x)))
                           (.attr "y2" (fn [_ idx]
                                         (-> (get link-dataset idx)
                                             .-target .-y)))))]

    ;; Add notes to simulation
    (-> sim
        (.nodes node-dataset)
        (.on "tick" tick-handler))

    ;; Add link force to simulation
    (-> sim
        (.force "link")
        (.links link-dataset))

    ;; Add collision force to simulation
    (-> sim
        (.force "collision"))))
