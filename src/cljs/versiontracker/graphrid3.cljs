(ns versiontracker.graphrid3
  (:require [rid3.core :as rid3]
            [re-frame.core :as rf]
            [versiontracker.events :as vt-even]
            [versiontracker.subs :as vt-subs]))

(declare sim-did-update)
(declare drag-started)
(declare dragged)
(declare drag-ended)

(enable-console-print!)
(def log (.-log js/console))

(declare convert-links)

(defn remove-svg []
  (-> js/d3
      (.selectAll "#graph svg")
      (.remove)))

(defn get-radius []
  (let [radius (/ @(rf/subscribe [::vt-subs/svg-width]) 45)]
    radius))

(defn force-viz [ratom]
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
                                                                       "S" "SubNode")))
                                          (.call (-> js/d3
                                                     (.drag)
                                                     (.on "start" drag-started)
                                                     (.on "drag" dragged)
                                                     (.on "end" drag-ended))))]
                                (rf/dispatch-sync [::vt-even/set-var :node-elems r])))
                 :prepare-dataset (fn [ratom]
                                    (-> @ratom
                                        (get :nodes)
                                        clj->js))}
                {:kind :elem-with-data
                 :tag "text"
                 :class "names"
                 :did-mount (fn [node ratom]
                              (let [r (-> node
                                          (.attr "font-size" "0.8rem")
                                          (.attr "font-weight" "bold")
                                          (.text (fn [d] (.-name d))))]
                                (rf/dispatch-sync [::vt-even/set-var :name-elems r])))
                 :prepare-dataset (fn [ratom]
                                    (-> @ratom
                                        (get :nodes)
                                        clj->js))}
                {:kind :elem-with-data
                 :tag "text"
                 :class "versions"
                 :did-mount (fn [node ratom]
                              (let [r (-> node
                                          (.attr "font-size" "0.8rem")
                                          (.text (fn [d] (.-version d))))]
                                (rf/dispatch-sync [::vt-even/set-var :vers-elems r])))
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
  (let [width (rf/dispatch [::vt-even/svg-width])
        height (rf/dispatch [::vt-even/svg-height])]
    [:div {:hidden @select-view}
     (main-panel)]))

(defn sim-did-update [ratom]
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
                                        (.radius (* 1.25 (get-radius))))))
        _ (rf/dispatch-sync [::vt-even/set-var :sim sim])
        node-dataset (clj->js (-> @ratom
                                  (get :nodes)))
        link-dataset (clj->js (-> @ratom
                                  (get :lines)))
        node-elems @(rf/subscribe [::vt-subs/get-var :node-elems])
        name-elems @(rf/subscribe [::vt-subs/get-var :name-elems])
        vers-elems @(rf/subscribe [::vt-subs/get-var :vers-elems])
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

                       (-> name-elems
                           (.attr "x" (fn [_ idx]
                                        (- (.-x (get node-dataset idx)) (/ radius 2))))
                           (.attr "y" (fn [_ idx]
                                        (- (.-y (get node-dataset idx)) 5))))

                       (-> vers-elems
                           (.attr "x" (fn [_ idx]
                                        (- (.-x (get node-dataset idx)) (/ radius 2))))
                           (.attr "y" (fn [_ idx]
                                        (+ (.-y (get node-dataset idx)) 7))))

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

(defn drag-started [event idx]
  (let [sim @(rf/subscribe [::vt-subs/get-var :sim])
        d (first (->> sim .nodes (filter #(= (.-id idx) (.-id %)))))]
    (log "Start-SIM: " sim)
    (log "Start-D: " d)
    (when (= 0 (-> event .-active))
      (-> sim (.alphaTarget 0.3) (.restart)))
    (set! (.-fx ^js d) (.-x d))
    (set! (.-fy ^js d) (.-y d))))


(defn dragged [event idx]
  (let [sim @(rf/subscribe [::vt-subs/get-var :sim])
        d (first (->> sim .nodes (filter #(= (.-id idx) (.-id %)))))]
    (log "Drag-SIM: " sim)
    (log "Drag-D: " d)
    (log "Drag-event: " event)
    (set! (.-fx ^js d) (.-x event))
    (set! (.-fy ^js d) (.-y event))))

(defn drag-ended [event idx]
  (let [sim @(rf/subscribe [::vt-subs/get-var :sim])
        d (first (->> sim .nodes (filter #(= (.-id idx) (.-id %)))))]
    (log "End-SIM: " sim)
    (log "End-D: " d)
    (when (= 0 (-> event .-active))
      (-> sim (.alphaTarget 0)))
    (set! (.-fx ^js d) nil)
    (set! (.-fy ^js d) nil)))
