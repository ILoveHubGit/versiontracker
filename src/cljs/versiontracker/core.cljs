(ns versiontracker.core
  (:require
    [kee-frame.core :as kf]
    [re-frame.core :as rf]
    [goog.dom :as g-dom]
    [goog.style :as g-sty]
    [versiontracker.ajax :as vt-ajax]
    [versiontracker.routing :as vt-rout]
    [versiontracker.view :as vt-view]
    [versiontracker.events :as vt-even]))

(def log (.-log js/console))

(kf/reg-controller
  ::about-controller
  {:params (constantly true)
   :start  [::vt-even/load-about-page]})

;; -------------------------
;; Initialize app
(defn ^:dev/after-load mount-components
  []
  (rf/clear-subscription-cache!)
  (kf/start! {:routes         vt-rout/routes
              :hash-routing?  true
              #_#_
              :log            {:level        :debug
                               :ns-blacklist ["kee-frame.event-logger"]}
              :initial-db     {}
              :root-component [vt-view/root-component]}))

(defn init! []
  (vt-ajax/load-interceptors!)
  (set! js/window.onresize (fn []
                             (rf/dispatch [::vt-even/svg-width])
                             (rf/dispatch [::vt-even/svg-height])))
  (mount-components))
