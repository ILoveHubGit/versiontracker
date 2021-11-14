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

(def color (.scaleOrdinal js/d3 (.-schemeCategory20 js/d3)))

(def boxwidth (atom 110))
(def boxheight (atom 55))


(log (str "Initial-Size: " @svg-width " " @svg-height))

(defn append-svg [width height]
  (-> js/d3
      (.select "#graph")
      (.append "svg")
      (.classed "graph" true)
      (.attr "width" width)
      (.attr "height" height)))

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

(defn ticked [lines nodes ids versions]
  (fn tick []
    (reset! svg-width (js/parseInt (-> js/d3
                                       (.select "svg")
                                       (.attr "width"))))
    (reset! svg-height (js/parseInt (-> js/d3
                                        (.select "svg")
                                        (.attr "height"))))
    (reset! radius (/ @svg-width 10))
    (reset! fontsize (/ @radius 4))
    (-> lines
        ; (.attr "x1" (fn [d] (Math/max (/ @radius 2)
        ;                               (Math/min (- @svg-width (/ @radius 2))
        ;                                         1))))
        ; (.attr "y1" (fn [d] (Math/max (/ @radius 2) (Math/min (- @svg-height (/ @radius 2)) 1))))
        ; (.attr "x2" (fn [d] (Math/max (/ @radius 2) (Math/min (- @svg-width (/ @radius 2)) 1))))
        ; (.attr "y2" (fn [d] (Math/max (/ @radius 2) (Math/min (- @svg-height (/ @radius 2)) 1)))))
        (.attr "x1" (fn [d] (Math/max (/ @radius 2)
                                      (Math/min (- @svg-width (/ @radius 2))
                                                (.-x1 d)))))
        (.attr "y1" (fn [d] (Math/max (/ @radius 2) (Math/min (- @svg-height (/ @radius 2)) (.. d -source -y)))))
        (.attr "x2" (fn [d] (Math/max (/ @radius 2) (Math/min (- @svg-width (/ @radius 2)) (.. d -target -x)))))
        (.attr "y2" (fn [d] (Math/max (/ @radius 2) (Math/min (- @svg-height (/ @radius 2)) (.. d -target -y))))))
    (-> nodes
      (.attr "r" @radius)
      (.attr "cx" (fn [d] (Math/max 0 (Math/min (- @svg-width @radius) (- (.-cx d) (/ @radius 2))))))
      (.attr "cy" (fn [d] (Math/max 0 (Math/min (- @svg-height @radius) (- (.-cy d) (/ @radius 2)))))))
    (-> ids
      (.attr "font-size" (str @fontsize "px"))
      (.attr "x" (fn [d] (Math/max 3 (Math/min (- @svg-width (- @radius 3)) (- 1 (/ @radius 2.2))))))
      (.attr "y" (fn [d] (Math/max @fontsize (Math/min (+ (- @svg-height @radius) @fontsize) (- 1 (- @fontsize 4)))))))
    (-> versions
      (.attr "font-size" (str @fontsize "px"))
      (.attr "x" (fn [d] (Math/max 3 (Math/min (- @svg-width (- @radius 3)) (- 1 (/ @radius 2.2))))))
      (.attr "y" (fn [d] (Math/max (* 2 @fontsize) (Math/min (+ (- @svg-height @radius) (* 2 @fontsize)) (+ 1 4))))))))

(defn simulation [graph defs lines nodes versions ids]
  (let [sim
        (-> js/d3
            (.forceSimulation)
            (.force "charge" (-> js/d3
                                 (.forceManyBody)
                                 (.strength (/ (* @svg-height @svg-width) -3000))))
            (.force "link" (-> js/d3
                               (.forceLink)
                               (.strength (/ (/ @svg-width @svg-height) 7.5))
                               (.id (fn [d] (.-id d)))))
            (.force "center" (.forceCenter js/d3
                                           (/ @svg-width 2)
                                           (/ @svg-height 2))))]
    (-> sim
        (.nodes (.-nodes graph))
        (.on "tick" (ticked lines nodes ids versions)))
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
  (let [defs     (-> svg
                     (.append "defs")
                     (.append "marker")
                     (.attr "id" "arrow")
                     (.attr "markerwidth" "4")
                     (.attr "markerheight" "4")
                     (.attr "orient" "auto")
                     (.attr "refY" "2")
                     (.append "path")
                     (.attr "d" "M0,0 L4,2 0,4"))
        conn    (-> svg
                    (.append "g")
                    (.attr "class" "links")
                    (.selectAll "g")
                    (.data (.-links graph))
                    (.enter)
                    (.append "g"))
        links  (-> conn
                   (.append "line")
                   (.attr "stroke-width" (fn [d] (* 3 (.-value d)))))
        ltexts  (-> conn
                    (.append "text")
                    (.text (fn [d] (str (.-source d) "->" (.-target d)))))
        apps    (-> svg
                    (.append "g")
                    (.attr "class" "nodes")
                    (.selectAll "g")
                    (.data (.-nodes graph))
                    (.enter)
                    (.append "g"))
        nodes   (-> apps
                    (.append "rect")
                    (.attr "width" boxwidth)
                    (.attr "height" boxheight)
                    (.attr "rx" 5)
                    (.attr "ry" 5)
                    (.attr "stroke" (fn [d] (color (.-group d))))
                    (.attr "fill" (fn [d] (str (.-fill d))))
                    (.call (-> (.drag js/d3)
                               (.on "start" drag-dispatcher)
                               (.on "drag" drag-dispatcher)
                               (.on "end" drag-dispatcher))))
        info       (-> apps
                       (.append "text"))
        versions (-> info
                     (.append "tspan")
                     (.text (fn [d] (str (.-version d) " - " (.-i d)))))
        ids      (-> info
                     (.append "tspan")
                     (.attr "font-weight" "bold")
                     (.text (fn [d] (.-id d))))
        idates   (-> info
                     (.append "tspan")
                     (.text (fn [d] (.-d d))))]
    [defs links ltexts nodes versions idates ids]))

(def mydata
  {
     :appname "iTest Epics to Applications"
     :nodes [
             {:id "CIM" :version "19.6.10" :i "2019-09" :d "CIMAQ4" :group 1 :fill "#ffff00"}
             {:id "Spider" :version "3.4" :i "2019-09" :d "KT13" :group 2 :fill "#aec700"}]
     :links [
             {:source "CIM" :target "Spider" :value 1}]})



(defn graph-view
  [links select-view]
  (let [window-size (g-sty/getSize (g-dom/getElement "graph"))
        width (.-width window-size)
        height (.-height window-size)
        svg (append-svg width height)
        data-channel (chan 1)
        data-error (chan 1)
        window-resized-channel (chan (sliding-buffer 5))]
    (handle-window-resize window-resized-channel)
    (put! data-channel mydata)
    (go
      (alt!
        data-error ([err] (throw err))
        data-channel ([data]
                      (log "Links: " (clj->js data))
                      (let [graph (clj->js data)
                            [defs lines nodes versions ids] (draw-graph svg graph)]
                        (reset! svg-width (js/parseInt (-> js/d3
                                                           (.select "svg")
                                                           (.attr "width"))))
                        (reset! svg-height (js/parseInt (-> js/d3
                                                            (.select "svg")
                                                            (.attr "height"))))
                        (swap! app-state
                               assoc :simulation (simulation graph defs lines nodes versions ids)))))
      (close! data-error)
      (close! data-channel))))
    ; [:div {:hidden @select-view}
    ;  [:div "Nodes: " (str (:nodes data))]
    ;  [:br]
    ;  [:div "Links: " (str (:lines data))]]))
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
