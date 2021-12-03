(ns versiontracker.graph
  (:require [cljsjs.d3]
            [cljs.core.async :refer [<! alt! chan close! go go-loop put! sliding-buffer]]
            [goog.dom :as g-dom]
            [goog.style :as g-sty]
            [goog.events :as g-eve]
            [goog.events.EventType :as EventType]
            [clojure.walk]
            [re-frame.core :as rf]))


(enable-console-print!)
(def log (.-log js/console))

(declare convert-links)

(def app-state
  (atom {:simulation nil}))

; (def window-resized-channel (chan (sliding-buffer 2)))
(def window-resized-channel (chan 1))

(def svg-width (atom 1000))
(def svg-height (atom 600))
(def radius (atom (/ @svg-width 50)))

(def link-strength (atom 1))
(def charge-strength (atom -50))
(def charge-distance (atom 10))

(defn remove-svg []
  (-> js/d3
      (.selectAll "#graph svg")
      (.remove)))

(defn get-svg-width []
  (let [width (min (.-width (g-dom/getViewportSize (g-dom/getWindow))) (.-width (g-sty/getTransformedSize (g-dom/getElement "graph"))))]
    ; (log "Width: " width)
    width))

(defn get-svg-height []
  (let [height (- (.-height (g-dom/getViewportSize (g-dom/getWindow))) (.-y (g-sty/getPosition (g-dom/getElement "graph"))))]
    ; (log "Height: " height)
    height))

(defn get-radius []
  (let [radius (/ (get-svg-width) 40)]
    ; (log "Radius: " radius)
    radius))

(defn reset-svg-size! []
  (do
    (reset! svg-width (get-svg-width))
    (reset! svg-height (get-svg-height))
    (reset! charge-strength (/ @svg-width -50))
    (reset! charge-distance (/ @svg-width 2))
    (reset! link-strength (/ @svg-width 750))))

(defn append-svg []
  (let [width (get-svg-width)
        height (get-svg-height)]
    (-> js/d3
        (.select "#graph")
        (.append "svg")
        (.classed "graph" true)
        (.attr "viewBox" (str "0 0 " width " " height))
        (.attr "preserveAspectRatio" "xMidYMid meet")
        (.attr "width" "100%")
        (.attr "height" "100%")
        (.attr "style" "background-color:rgb(245,245,245)"))))

(defn refresh-simulation-force []
  (let [sim (:simulation @app-state)]
    (reset-svg-size!)
    (.force sim "center" (.forceCenter js/d3
                                       (/ (get-svg-width) 2)
                                       (/ (get-svg-height) 2)))
    (-> sim
        (.alphaTarget 0.1)
        (.restart))))

  ; d3.select(window).on("resize", resize))

(defn handle-window-resize []
  ;;subscribe to window.resize
  (g-eve/listen js/window
                 EventType/RESIZE
                 (fn [event]
                   (put! window-resized-channel event)))
  ;; handle resize
  (go []
    (let [event (<! window-resized-channel)]
      (when event
        (refresh-simulation-force)))))


(defn ticked [svg lines nodes versions ids]
  (let [actrad (get-radius)]
    (fn tick []
      (-> svg)
          ; (.attr "width" @svg-width)
          ; (.attr "height" @svg-height))
           ; (.attr "aaa" (fn [d] (.. d -nodes -x))))
      (-> lines
          (.attr "x1" (fn [d] (.. d -source -x)))
          (.attr "y1" (fn [d] (.. d -source -y)))
          (.attr "x2" (fn [d] (.. d -target -x)))
          (.attr "y2" (fn [d] (.. d -target -y))))
      (-> nodes
        (.attr "r" (fn [d] (case (str (first (.-id d)))
                                 "I" (* 0.8 actrad)
                                 "N" (* 1.2 actrad)
                                 "S" actrad)))
        (.attr "cx" (fn [d] (Math/max (* 1.2 actrad) (Math/min (get-svg-width) (.-x d)))))
        (.attr "cy" (fn [d] (Math/max (* 1.2 actrad) (Math/min (get-svg-height) (.-y d))))))
      (-> ids
        (.attr "x" (fn [d] (- (.-x d) (/ actrad 2))))
        (.attr "y" (fn [d] (- (.-y d) 3))))
      (-> versions
        (.attr "x" (fn [d] (- (.-x d) (/ actrad 2))))
        (.attr "y" (fn [d] (+ (.-y d) 7)))))))

(defn simulation [graph svg svg-defs lines nodes versions ids]
  (let [sim
        (-> js/d3
            (.forceSimulation)
            (.force "charge" (-> js/d3
                                 (.forceManyBody)
                                 (.strength -50)
            ; ;                      ; ; ; (.theta 0.5)
                                 (.distanceMax @charge-distance)))
            (.force "link" (-> js/d3
                               (.forceLink)
                               (.strength @link-strength)
                               (.id (fn [d] (.-id d)))))
            (.force "center" (.forceCenter js/d3
                                           (/ (get-svg-width) 2)
                                           (/ (get-svg-height) 2)))
            (.force "collision" (-> js/d3
                                    (.forceCollide)
                                    (.radius (* 1.2 (get-radius))))))
        xnodes (map #(.-groups %) nodes)]
    (-> sim
        (.nodes (.-nodes graph))
        (.on "tick" (ticked svg lines nodes versions ids)))
    (-> sim
        (.force "link")
        (.links (.-lines graph)))
    (-> sim
        (.force "collision"))
    sim))

; (defmulti drag (fn [event d idx group] (.-type event)))
(defmulti drag (fn [event d] (.-type event)))

; (defmethod drag "start" [event d idx group])
(defmethod drag "start" [event d]
  (if (zero? (.-active event))
    (-> (:simulation @app-state)
        (.alphaTarget 0.3)
        (.restart))))

; (defmethod drag "drag" [event d idx group])
(defmethod drag "drag" [event d]
  (set! (.-x d) (.-x event))
  (set! (.-y d) (.-y event)))

; (defmethod drag "end" [event d idx group])
(defmethod drag "end" [event d]
  (if (zero? (.-active event))
    (.alphaTarget (:simulation @app-state) 0))
  (set! (.-x d) nil)
  (set! (.-y d) nil))

; (defmethod drag :default [event d idx group] nil)
(defmethod drag :default [event d] nil)

; (defn drag-dispatcher [d idx group]
;   (drag (.-event js/d3) d idx group))
(defn drag-dispatcher [d]
  (drag (.-event js/d3) d))

(defn draw-graph [svg graph]
  (let [svg-defs    (-> svg
                        (.append "defs")
                        (.append "marker")
                        (.attr "id" "arrow")
                        (.attr "markerwidth" "4")
                        (.attr "markerheight" "4")
                        (.attr "orient" "auto")
                        (.attr "refY" "2")
                        (.append "path")
                        (.attr "d" "M0,0 L4,2 0,4"))
        conn     (-> svg
                     (.append "g")
                     (.attr "class" "lines")
                     (.selectAll "g")
                     (.data (.-lines graph))
                     (.enter)
                     (.append "g"))
        lines    (-> conn
                     (.append "line")
                     (.attr "stroke-width" 2))
                     ; (.attr "marker-end" "url(#arrow)"))
        items    (-> svg
                     (.append "g")
                     (.attr "class" "nodes")
                     (.selectAll "g")
                     (.data (.-nodes graph))
                     (.enter)
                     (.append "g"))
        nodes    (-> items
                     (.append "circle")
                     (.attr "id" "Node")
                     (.attr "cx" 5)
                     (.attr "cy" 5)
                     (.attr "class" (fn [d] (case (str (first (.-id d)))
                                                  "I" "Interface"
                                                  "N" "Node"
                                                  "S" "SubNode")))
                     (.attr "stroke" "#aabbcc")
                     (.attr "fill" "#aabbcc")
                     (.call (-> (.drag js/d3)
                                (.on "start" drag-dispatcher)
                                (.on "drag" drag-dispatcher)
                                (.on "end" drag-dispatcher))))
        info     (-> items
                     (.append "text"))
        versions (-> info
                     (.append "tspan")
                     (.attr "font-size" "0.6rem")
                     (.text (fn [d] (str (.-version d)))))
        ids      (-> info
                     (.append "tspan")
                     (.attr "font-size" "0.6rem")
                     (.attr "font-weight" "bold")
                     (.text (fn [d] (.-name d))))]
    [svg-defs lines nodes versions ids]))


(defn graph-view
  [select-view]
  (let [clinks (convert-links @(rf/subscribe [:links]))
        ; size (reset-svg-size!)
        svg (append-svg)
        data-channel (chan 1)
        data-error (chan 1)]
    ; (handle-window-resize)
    (put! data-channel clinks)
    (go
      (alt!
        data-error ([err] (throw err))
        data-channel ([data]
                      ; (log "Links: " (clj->js data))
                      (let [graph (clj->js data)
                            [svg-defs lines nodes versions ids] (draw-graph svg graph)]
                        (reset-svg-size!)
                        (log "GV2-Size: " (get-svg-width) " - " (get-svg-height) " - " (get-radius))
                        (swap! app-state
                               assoc :simulation (simulation graph svg svg-defs lines nodes versions ids)))))
                        ; (refresh-simulation-force))))
      (close! data-error)
      (close! data-channel))
    [:div {:hidden @select-view}]))
     ; [:div "Size: " (get-svg-width) " - " (get-svg-height)]]))
     ; [:div "Nodes: " (str (:nodes clinks))]
     ; [:br]
     ; [:div "Links: " (str (:lines clinks))]]))
          ; [:br]
          ; [:div "All: " (str defs lines nodes versions ids)]]))))


(defn add-nodes
  [link]
  (let [source (:source link)
        target (:target link)]
    [{:id (str "I" (:id link))
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
     :type "ns"}))

(defn add-in-link
  [side link]
  (let [node   (side link)
        nod-id (if (nil? (:SubNode node))
                 (str "N" (:id node))
                 (str "S" (:subid node)))
        intf   (if (= side :source)
                 :target
                 :source)]
    (when (> (count nod-id) 1)
      {side nod-id
       intf (str "I" (:id link))
       :type (:type link)})))

(defn convert-links
  [links]
  (when-not (= links "No data")
   {:nodes
    (->> links
        (mapv #(add-nodes %))
        flatten
        set
        (filterv #(not (nil? %))))
    :lines
    (filterv #(not (nil? %))
      (set
        (concat
          (mapv #(add-ns-link :source %) links)
          (mapv #(add-ns-link :target %) links)
          (mapv #(add-in-link :source %) links)
          (mapv #(add-in-link :target %) links))))}))
