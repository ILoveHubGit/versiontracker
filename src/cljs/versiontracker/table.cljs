(ns versiontracker.table
  (:require [clojure.string :as c-str]))


(defn make-row
  [link]
  (let [type (c-str/lower-case (:type link))]
    [:tr
     [:td (:Node (:source link))]
     [:td (:Version (:source link))]
     [:td (:SubNode (:source link))]
     [:td (:SubVersion (:source link))]
     [:td [:abbr {:title (str "Deployed on: " (:deploymentdate link))} (:name link)]]
     [:td (:version link)]
     [:td [:img {:src (str "/img/" type ".svg") :title type :width 32}]]
     [:td (:insertdate link)]
     [:td (:Node (:target link))]
     [:td (:Version (:target link))]
     [:td (:SubNode (:target link))]
     [:td (:SubVersion (:target link))]]))

(defn table-view
  [links select-view]
  [:div {:hidden (not @select-view)}
   (if (= links "No data")
     [:div "No data available for this environment"]
     [:div.table-container
      [:table.table.is-bordered.is-striped.is-narrow.is-hoverable
       [:thead
        [:tr.color-blue
         [:th.has-text-centered.has-text-white {:colSpan 4} "Source"]
         [:th.has-text-centered.has-text-white {:colSpan 4} "Interface"]
         [:th.has-text-centered.has-text-white {:colSpan 4} "Target"]]
        [:tr.color-blue
         [:th.has-text-white "Node"]
         [:th.has-text-white "Version"]
         [:th.has-text-white "Sub Node"]
         [:th.has-text-white "Sub Version"]
         [:th.has-text-white "Interface"]
         [:th.has-text-white "Version"]
         [:th.has-text-white "Type"]
         [:th.has-text-white "Date"]
         [:th.has-text-white "Node"]
         [:th.has-text-white "Version"]
         [:th.has-text-white "Sub Node"]
         [:th.has-text-white "Sub Version"]]]
       [:tbody
        (map #(make-row %) links)]]])])
