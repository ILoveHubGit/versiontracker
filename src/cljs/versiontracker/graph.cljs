(ns versiontracker.graph
  (:require [clojure.string :as c-str]
            [cljsjs.d3]
            [cljs.core.async :refer [<! alt! chan close! go go-loop put! sliding-buffer]]
            [goog.dom :as g-dom]
            [goog.style :as g-sty]
            [goog.events :as g-eve]
            [goog.events.EventType :as EventType]
            [clojure.walk]))

(enable-console-print!)
(def log (.-log js/console))

(declare convert-links)

(def app-state
  (atom {:simulation nil}))

(def svg-width (atom 100))
(def svg-height (atom 100))
(def radius (atom 20))
(def fontsize (atom 12))

(log (str "Initial-Size: " @svg-width " " @svg-height))

(defn append-svg [width height]
  (-> js/d3
      (.select "#graph")
      (.append "svg")
      (.classed "graph" true)
      (.attr "width" width)
      (.attr "height" height)))
      ; (.attr "style" "background-color:gray")))

(defn remove-svg []
  (-> js/d3
      (.selectAll "#graph svg")
      (.remove)))

(defn refresh-simulation-force [event]
  (let [window-size (g-sty/getSize (g-dom/getElement "graph"))
        width  (.-width window-size)
        height (.-height window-size)
        sim (:simulation @app-state)]

    (.force sim "center" (.forceCenter js/d3 (/ width 2) (/ height 2)))

    (-> sim
        (.alphaTarget 0.3)
        (.restart))))

(defn handle-window-resize [ch]
  ;;subscribe to window.resize
  (g-eve/listen js/window
                 EventType/RESIZE
                 (fn [event]
                   (put! ch event)))
  ;; handle resize
  (go-loop [event (<! ch)]
    (when event
      (refresh-simulation-force event))
    (recur (<! ch))))

(defn ticked [lines nodes versions ids]
  (fn tick []
    (reset! svg-width (js/parseInt (-> js/d3
                                       (.select "svg")
                                       (.attr "width"))))
    (reset! svg-height (js/parseInt (-> js/d3
                                        (.select "svg")
                                        (.attr "height"))))
    (reset! radius (/ @svg-width 30))
    (reset! fontsize (/ @radius 3))
    (-> lines
        ; (log "d: " (fn [d] (.. d -source -x)))
        (.attr "x1" (fn [d] (.. d -source -x)))
        (.attr "y1" (fn [d] (.. d -source -y)))
        (.attr "x2" (fn [d] (.. d -target -x)))
        (.attr "y2" (fn [d] (.. d -target -y))))
    (-> nodes
      (.attr "r" @radius)
      (.attr "cx" (fn [d] (Math/max (* @radius 2) (Math/min (.-x d) (- @svg-width (* @radius 2))))))
      (.attr "cy" (fn [d] (Math/max (* @radius 2) (Math/min (.-y d) (- @svg-height (* @radius 2)))))))
    (-> ids
      (.attr "font-size" (str @fontsize "px"))
      (.attr "x" (fn [d] (Math/max (* @radius 1.5) (Math/min (- (.-x d) @fontsize)
                                                             (- @svg-width (* @radius 2.5))))))
      (.attr "y" (fn [d] (Math/max (* @radius 2) (Math/min (.-y d)
                                                           (- @svg-height (* @radius 2)))))))
    (-> versions
      (.attr "font-size" (str @fontsize "px"))
      (.attr "x" (fn [d] (Math/max (* @radius 1.5) (Math/min (- (.-x d) @fontsize)
                                                             (- @svg-width (* @radius 2.5))))))
      (.attr "y" (fn [d] (Math/max (* @radius 1.5) (Math/min (+ (.-y d) @fontsize) (- @svg-height (* @radius 1.5)))))))))

(defn simulation [graph svg-defs lines nodes versions ids]
  (let [sim
        (-> js/d3
            (.forceSimulation)
            (.force "charge" (-> js/d3
                                 (.forceManyBody)
                                 (.strength (/ (* @svg-height @svg-width) -5000))))
            (.force "link" (-> js/d3
                               (.forceLink)
                               (.strength (/ (/ @svg-width @svg-height) 10))
                               (.id (fn [d] (.-id d)))))
            (.force "center" (.forceCenter js/d3
                                           (/ @svg-width 2)
                                           (/ @svg-height 2))))]
    (-> sim
        (.nodes (.-nodes graph))
        (.on "tick" (ticked lines nodes versions ids)))
    (-> sim
        (.force "link")
        (.links (.-lines graph)))
    sim))

(defmulti drag (fn [event d idx group] (.-type event)))

(defmethod drag "start" [event d idx group]
  (if (zero? (.-active event))
    (-> (:simulation @app-state)
        (.alphaTarget 0.3)
        (.restart))))

(defmethod drag "drag" [event d idx group]
  (set! (.-fx d) (.-x event))
  (set! (.-fy d) (.-y event)))

(defmethod drag "end" [event d idx group]
  (if (zero? (.-active event))
    (.alphaTarget (:simulation @app-state) 0))
  (set! (.-fx d) nil)
  (set! (.-fy d) nil))

(defmethod drag :default [event d idx group] nil)

(defn drag-dispatcher [d idx group]
  (drag (.-event js/d3) d idx group))

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
                     (.attr "stroke-width" 3))
        ltexts  (-> conn
                    (.append "text")
                    (.text (fn [d] (str (.-source d) "->" (.-target d)))))
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
                     (.attr "r" @radius)
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
                     (.text (fn [d] (str (.-version d)))))
        ids      (-> info
                     (.append "tspan")
                     (.attr "font-weight" "bold")
                     (.text (fn [d] (.-name d))))]
    [svg-defs lines nodes versions ids]))


(defn graph-view
  [links select-view]
  (let [clinks (convert-links links)
        window-size (g-sty/getSize (g-dom/getElement "graph"))
        width (.-width window-size)
        height (.-height window-size)
        svg (append-svg width height)
        data-channel (chan 1)
        data-error (chan 1)
        window-resized-channel (chan (sliding-buffer 5))]
    (handle-window-resize window-resized-channel)
    (put! data-channel clinks)
    (go
      (alt!
        data-error ([err] (throw err))
        data-channel ([data]
                      (log "Links: " (clj->js data))
                      (let [graph (clj->js data)
                            [svg-defs lines nodes versions ids] (draw-graph svg graph)]
                        (reset! svg-width (js/parseInt (-> js/d3
                                                           (.select "svg")
                                                           (.attr "width"))))
                        (reset! svg-height (js/parseInt (-> js/d3
                                                            (.select "svg")
                                                            (.attr "height"))))
                        (swap! app-state
                               assoc :simulation (simulation graph svg-defs lines nodes versions ids))))))

    [:div {:hidden @select-view}]))
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
