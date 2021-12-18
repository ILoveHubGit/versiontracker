(ns versiontracker.export.pdf
  (:require [clj-pdf.core :as cpdf]
            [versiontracker.db.data :as vt-data]
            [clojure.tools.logging :as log]))

(def pdf-report (java.io.ByteArrayOutputStream.))

(defn make-rows
  [env-name date]
  (let [content (vt-data/ret-links env-name date)]
    (for [link content]
      (let [source (:source link)
            target (:target link)]
        [(:Node source) (:Version source) (:SubNode source) (:SubVersion source)
         (:name link) (:version link) (:type link) (:insertdate link)
         (:Node target) (:Version target) (:SubNode target) (:SubVersion target)]))))

(defn make-pdf
  [env-name date]
  (cpdf/pdf
    [{:title "Environment overview"
      :footer {:text "Created by Version Tracker   Page"
               :footer-separator " - "}
      :pages true
      :orientation :landscape
      :author "Version Tracker"}
     [:heading {:style {:align :center}} (str "Environment: " env-name)]
     [:heading {:style {:align :center :size 12}} (str "Created on: " (new java.util.Date))]
     (when-not (nil? date)
       [:heading {:style {:align :center :size 10}} (str "History view for date: " date)])
     [:paragraph " "]
     (into [:pdf-table
            {:header
             [[[:pdf-cell {:colspan 4
                           :border-color [255 255 255]
                           :color [255 255 255]
                           :background-color [62 142 208]}
                [:paragraph {:align :center :style :bold :size 9} "Source"]]
               [:pdf-cell {:colspan 4
                           :border-color [255 255 255]
                           :color [255 255 255]
                           :background-color [62 142 208]}
                [:paragraph {:align :center :style :bold :size 9} "Interface"]]
               [:pdf-cell {:colspan 4
                           :border-color [255 255 255]
                           :color [255 255 255]
                           :background-color [62 142 208]}
                [:paragraph {:align :center :style :bold :size 9} "Target"]]]
              [[:pdf-cell {:style :bold :size 9 :border-color [255 255 255] :color [255 255 255] :background-color [62 142 208]} "Application"]
               [:pdf-cell {:style :bold :size 9 :border-color [255 255 255] :color [255 255 255] :background-color [62 142 208]} "Version"]
               [:pdf-cell {:style :bold :size 9 :border-color [255 255 255] :color [255 255 255] :background-color [62 142 208]} "Function"]
               [:pdf-cell {:style :bold :size 9 :border-color [255 255 255] :color [255 255 255] :background-color [62 142 208]} "Sub Version"]
               [:pdf-cell {:style :bold :size 9 :border-color [255 255 255] :color [255 255 255] :background-color [62 142 208]} "Interface"]
               [:pdf-cell {:style :bold :size 9 :border-color [255 255 255] :color [255 255 255] :background-color [62 142 208]} "Version"]
               [:pdf-cell {:style :bold :size 9 :border-color [255 255 255] :color [255 255 255] :background-color [62 142 208]} "Type"]
               [:pdf-cell {:style :bold :size 9 :border-color [255 255 255] :color [255 255 255] :background-color [62 142 208]} "Date"]
               [:pdf-cell {:style :bold :size 9 :border-color [255 255 255] :color [255 255 255] :background-color [62 142 208]} "Application"]
               [:pdf-cell {:style :bold :size 9 :border-color [255 255 255] :color [255 255 255] :background-color [62 142 208]} "Version"]
               [:pdf-cell {:style :bold :size 9 :border-color [255 255 255] :color [255 255 255] :background-color [62 142 208]} "Function"]
               [:pdf-cell {:style :bold :size 9 :border-color [255 255 255] :color [255 255 255] :background-color [62 142 208]} "Sub Version"]]]
             :width-percent 100
             :size 7
             :family :sans-serif
             :border-color [150 150 150]
             :color [0 0 0]
             :background-color [255 255 255]}
            [11 7.9 8.4 8 8.6 7.9 6 11 11 7.9 8.4 8]]
          (make-rows env-name date))]
    (str env-name ".pdf")))

(defn create-pdf
  [env-name date]
  (log/info (str "create-pdf | Creation of a PDF has started with environment: " env-name " and date: " date))
  (let [result (make-pdf env-name date)]
    (clojure.java.io/file (str env-name ".pdf"))))
