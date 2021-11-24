(ns versiontracker.view
  (:require [clojure.string :as c-str]
            [kee-frame.core :as kf]
            [reagent.core :as r]
            [re-frame.core :as rf]
            [versiontracker.about :as vt-abou]
            [versiontracker.validation :as vt-vali]
            [versiontracker.forms.controls :as vt-form]
            [versiontracker.table :as vt-tabl]
            [versiontracker.graph :as vt-grap]))

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
      [:a {:href "/"} [:img.vertical-center {:src "/img/vt-logo.svg" :title "VT" :width 200}]]
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

(defn footer []
  [:footer.myFooter.is-light
   [:div.content.has-text-centered
     "© 2021 ILoveHubGit: Version Tracker Version: 0.2.1"]])

(defn about-page []
  [:section.section>div.container>div.content
   [:img {:src "/img/warning_clojure.png"}]])

(def select-view (r/atom true))

(defn home-page []
  [:section.section
   [:div.columns
    [:div.column.is-narrow {:id "search"}
     [:div.box
      [vt-form/dropdown :environments [:name] [:id]
       (into [] (concat [{:id 0 :name "Choose your environment ..."}] @(rf/subscribe [:environments])))
       :id :name
       {:label "Environments" :field-classes ["is-info is-light"]}]
      [vt-form/text-input :environments [:date] ::vt-vali/date "Wrong date format"
       {:label "Date" :field-classes []}]
      [:div.columns
        [:div.column
         [:button.button.is-info
          {:on-click #(rf/dispatch [:ret-links])}
          "Get Interfaces"]]
        [:div.column
         (when-not (or (nil? @(rf/subscribe [:links]))
                       (= "No data" @(rf/subscribe [:links])))
          [:div [:a {:href @(rf/subscribe [:ret-pdf]) :target "_blank"} [:button.button.is-info "PDF"]]])]]]]
    [:div.column
     ; [:div.container
     ;  [:div.content
       (when-let [links @(rf/subscribe [:links])]
        [:div.box
         [:div.tabs {:id "tabs"}
          [:ul
           [:li {:class (when @select-view "is-active")} [:a {:on-click (fn [] (reset! select-view true) (log "Table-view: " @select-view))} "Table View"]]
           [:li {:class (when-not @select-view "is-active")} [:a {:on-click (fn [] (reset! select-view false) (log "Graph-view: " @select-view))} "Graph View"]]]]
         [:div {:id "graph"}
           (if @select-view
             (do
               (vt-grap/remove-svg)
               (vt-tabl/table-view links select-view))
             (do
               (vt-grap/remove-svg)
               (vt-grap/graph-view select-view)))]])]]])


(defn root-component []
  [:div
   [navbar]
   [kf/switch-route (fn [route] (get-in route [:data :name]))
    :home home-page
    :about vt-abou/about-page
    nil [:div ""]]
   [footer]])
