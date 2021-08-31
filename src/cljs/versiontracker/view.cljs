(ns versiontracker.view
  (:require [clojure.string :as c-str]
            [kee-frame.core :as kf]
            [markdown.core :refer [md->html]]
            [reagent.core :as r]
            [re-frame.core :as rf]
            [versiontracker.validation :as vt-vali]
            [versiontracker.forms.controls :as forms]))

(def log (.-log js/console))

(defn nav-link [title page]
  [:a.navbar-item
   {:href   (kf/path-for [page])
    :class (when (= page @(rf/subscribe [:nav/page])) "is-active")}
   title])

(defn navbar []
  (r/with-let [expanded? (r/atom false)]
    [:nav.navbar.is-info>div.container
     [:div.navbar-brand
      [:a.navbar-item {:href "/" :style {:font-weight :bold}} "Version Tracker"]
      [:span.navbar-burger.burger
       {:data-target :nav-menu
        :on-click #(swap! expanded? not)
        :class (when @expanded? :is-active)}
       [:span][:span][:span]]]
     [:div#nav-menu.navbar-menu
      {:class (when @expanded? :is-active)}
      [:div.navbar-start
       [nav-link "Home" :home]
       [nav-link "About" :about]]]]))

(defn about-page []
  [:section.section>div.container>div.content
   [:img {:src "/img/warning_clojure.png"}]])

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

(def select-view (r/atom true))

(defn table-view
  [links]
  [:div {:hidden (not @select-view)}
   [:div.table-container
    [:table.table.is-bordered.is-striped.is-narrow.is-hoverable
     [:thead
      [:tr.color-blue
       [:th.has-text-centered.has-text-white {:colSpan 4} "Source"]
       [:th.has-text-centered.has-text-white {:colSpan 4} "Interface"]
       [:th.has-text-centered.has-text-white {:colSpan 4} "Target"]]
      [:tr.color-blue
       [:th.has-text-white "Application"]
       [:th.has-text-white "Version"]
       [:th.has-text-white "Function"]
       [:th.has-text-white "Sub Version"]
       [:th.has-text-white "Interface"]
       [:th.has-text-white "Version"]
       [:th.has-text-white "Type"]
       [:th.has-text-white "Date"]
       [:th.has-text-white "Application"]
       [:th.has-text-white "Version"]
       [:th.has-text-white "Function"]
       [:th.has-text-white "Sub Version"]]]
     [:tbody
      (map #(make-row %) links)]]]])

(defn graph-view
  [links]
  [:div {:hidden @select-view}
    [:div "Hier komt de graph"]])


(defn home-page []
  [:section.section>div.container>div.content
   [:div.columns
    [:div.column
      [forms/dropdown :environments [:name] [:id]
       (into [] (concat [{:id 0 :name "Choose your environment ..."}] @(rf/subscribe [:environments])))
       :id :name
       {:label "Environments" :field-classes ["required"]}]]
    [:div.column
     [forms/text-input :environments [:date] ::vt-vali/date "Wrong date format"
      {:label "Date" :field-classes ["required"]}]]
    [:div.column
     [:button.button.is-info
      {:on-click #(rf/dispatch [:ret-links])}
      "Get Interfaces"]]]
   (when-let [links @(rf/subscribe [:links])]
     (log "Select-view: " @select-view)
     [:div
      [:div.tabs
       [:ul
        [:li.is-active [:a {:on-click (fn [] (reset! select-view true) (log "Table-view: " @select-view))} "Table View"]]
        [:li [:a {:on-click (fn [] (reset! select-view false) (log "Graph-view: " @select-view))} "Graph View"]]]]
      (if @select-view
        (table-view links)
        (graph-view links))])])




(defn root-component []
  [:div
   [navbar]
   [kf/switch-route (fn [route] (get-in route [:data :name]))
    :home home-page
    :about about-page
    nil [:div ""]]])
