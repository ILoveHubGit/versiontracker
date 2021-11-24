(ns versiontracker.core
  (:require
    [kee-frame.core :as kf]
    [re-frame.core :as rf]
    [ajax.core :as http]
    [versiontracker.ajax :as ajax]
    [versiontracker.routing :as routing]
    [versiontracker.view :as view]
    [versiontracker.forms.core :as forms]))

(def log (.-log js/console))

;; -------------------------
;; Default error handler
;; -------------------------
(rf/reg-sub
  :set-error
  (fn [db _]
     (:errors db)))

(kf/reg-event-fx
  :set-error
  (fn [{:keys [db]} [_ request-type response]]
     {:db (assoc db :errors request-type :error-response response)}))

(rf/reg-event-fx
  ::load-about-page
  (constantly nil))

(kf/reg-controller
  ::about-controller
  {:params (constantly true)
   :start  [::load-about-page]})

(rf/reg-sub
  :environments
  (fn [db _]
    (:environments db)))

(rf/reg-sub
  :links
  (fn [db _]
    (:links db)))

(rf/reg-sub
  :ret-links
  (fn [db _]
    (:ret-links db)))

(rf/reg-sub
  :ret-pdf
  (fn [db _]
    (:ret-pdf db)))

(rf/reg-sub
  :window-width
  (fn [db _]
    (:window-width db)))

(rf/reg-sub
  :window-height
  (fn [db _]
    (:window-height db)))

(rf/reg-event-fx
 :window-width
 (fn [{:keys [db]} [_ width]]
   {:db (-> db
            (assoc-in [:test-data :width] width))}))

(rf/reg-event-fx
 :window-height
 (fn [{:keys [db]} [_ width]]
   {:db (-> db
            (assoc-in [:test-data :height] width))}))

(kf/reg-chain
  :ret-links
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
      {:db (assoc db :links (sort-by :name links))}
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

;; -------------------------
;; Initialize app
(defn ^:dev/after-load mount-components
  []
  (rf/clear-subscription-cache!)
  (kf/start! {:routes         routing/routes
              :hash-routing?  true
              #_#_
              :log            {:level        :debug
                               :ns-blacklist ["kee-frame.event-logger"]}
              :initial-db     {}
              :root-component [view/root-component]}))

(defn init! []
  (ajax/load-interceptors!)
  (mount-components))
