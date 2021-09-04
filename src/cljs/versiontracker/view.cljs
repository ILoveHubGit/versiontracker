(ns versiontracker.view
  (:require [clojure.string :as c-str]
            [kee-frame.core :as kf]
            [reagent.core :as r]
            [re-frame.core :as rf]
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

(defn footer []
  [:footer.myFooter
   [:div.content.has-text-centered
     "© 2021 ILoveHubGit Version: 0.1.0"]])

(defn about-page []
  [:section.section>div.container>div.content
   [:img {:src "/img/warning_clojure.png"}]])

(def select-view (r/atom true))

(defn home-page []
  [:section.section>div.container>div.content
   [:div.columns
    [:div.column
      [vt-form/dropdown :environments [:name] [:id]
       (into [] (concat [{:id 0 :name "Choose your environment ..."}] @(rf/subscribe [:environments])))
       :id :name
       {:label "Environments" :field-classes ["required"]}]]
    [:div.column
     [vt-form/text-input :environments [:date] ::vt-vali/date "Wrong date format"
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
        [:li {:class (when @select-view "is-active")} [:a {:on-click (fn [] (reset! select-view true) (log "Table-view: " @select-view))} "Table View"]]
        [:li {:class (when-not @select-view "is-active")} [:a {:on-click (fn [] (reset! select-view false) (log "Graph-view: " @select-view))} "Graph View"]]]]
      (if @select-view
        (vt-tabl/table-view links select-view)
        (vt-grap/graph-view links select-view))])])


(defn root-component []
  [:div
   [navbar]
   [kf/switch-route (fn [route] (get-in route [:data :name]))
    :home home-page
    :about about-page
    nil [:div ""]]
   [footer]])
