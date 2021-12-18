(ns versiontracker.subs
  (:require [re-frame.core :as rf]))

;; -------------------------
;; Default error handler
;; -------------------------
(rf/reg-sub
  ::set-error
  (fn [db _]
     (:errors db)))

(rf/reg-sub
  ::environments
  (fn [db _]
    (:environments db)))

(rf/reg-sub
  ::links
  (fn [db _]
    (:links db)))

(rf/reg-sub
  ::clinks
  (fn [db _]
    (:clinks db)))

(rf/reg-sub
  ::get-var
  (fn [db [_ var-key]]
    (get db var-key)))        

(rf/reg-sub
  ::svg-width
  (fn [db _]
    (:svg-width (:clinks db))))

(rf/reg-sub
  ::svg-height
  (fn [db _]
    (:svg-height (:clinks db))))

(rf/reg-sub
  ::ret-links
  (fn [db _]
    (:ret-links db)))

(rf/reg-sub
  ::ret-pdf
  (fn [db _]
    (:ret-pdf db)))
