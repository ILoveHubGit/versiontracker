(ns versiontracker.events
  (:require [kee-frame.core :as kf]
            [ajax.core :as http]
            [goog.dom :as g-dom]
            [goog.style :as g-sty]
            [versiontracker.forms.core :as forms]
            [versiontracker.utils :as vt-util]))

(def log (.-log js/console))

(kf/reg-event-fx
  ::set-error
  (fn [{:keys [db]} [_ request-type response]]
     {:db (assoc db :errors request-type :error-response response)}))

(kf/reg-event-fx
  ::load-about-page
  (constantly nil))

(kf/reg-event-fx
 ::set-var
 (fn [{:keys [db]} [var-key d3-node]]
   {:db (-> db
            (assoc var-key d3-node))}))

(kf/reg-event-fx
  ::svg-width
  (fn [{:keys [db]} [_]]
    {:db (-> db
             (assoc-in [:clinks :svg-width] (min (.-width (g-dom/getViewportSize (g-dom/getWindow))) (.-width (g-sty/getTransformedSize (g-dom/getElement "graph"))))))}))

(kf/reg-event-fx
  ::svg-height
  (fn [{:keys [db]} [_]]
    {:db (-> db
             (assoc-in [:clinks :svg-height] (- (.-height (g-dom/getViewportSize (g-dom/getWindow))) (.-y (g-sty/getPosition (g-dom/getElement "graph"))))))}))

(kf/reg-chain
  ::ret-links
  (fn [{:keys [db]}]
    {:db (dissoc db :links :ret-pdf :errors)})
  (fn [{:keys [db]} [_]]
    (let [formfields (get-in db (concat forms/value-db-path [:environments]))
          env_name (:name (first (:id formfields)))
          date (:date formfields)
          uri (if (or (nil? env_name)
                      (= env_name "Choose your environment ..."))
                nil
                (str "/api/environments/" env_name "/links/" env_name ".pdf"
                     (when-not (nil? date) (str "?date=" date))))]
      (if (nil? uri)
        {:db (dissoc db :ret-pdf)}
        {:db (assoc db :ret-pdf uri)})))
  (fn [{:keys [db]} [_]]
    (let [formfields (get-in db (concat forms/value-db-path [:environments]))
          env_name (:name (first (:id formfields)))
          date (:date formfields)
          uri (if (or (nil? env_name)
                      (= env_name "Choose your environment ..."))
                nil
                (str "/api/environments/" env_name "/links"
                     (when-not (nil? date) (str "?date=" date))))]
      (log "env_name " env_name)
      (log "URI: " uri)
      (if (nil? uri)
        nil
        {:http-xhrio {:method          :get
                      :uri             uri
                      :headers         {"Accept" "application/json"}
                      :response-format (http/json-response-format {:keywords? true})
                      :on-failure      [:set-error ::ret-links]}})))
  (fn [{:keys [db]} [links]]
    (if (seq links)
      {:db (assoc db :links (sort-by :name links)
                     :clinks (vt-util/convert-links links))}
      {:db (assoc db :links "No data")})))

(kf/reg-chain
  ::load-home-page
  (fn [_ _]
    {:http-xhrio {:method          :get
                  :uri             "/api/environments"
                  :headers         {"Accept" "application/json"}
                  :response-format (http/json-response-format {:keywords? true})
                  :on-failure      [:common/set-error]}})
  (fn [{:keys [db]} [_ environments]]
    {:db (assoc db :environments (vec (sort-by :name environments)))}))


(kf/reg-controller
  ::home-controller
  {:params (constantly true)
   :start  [::load-home-page]})
