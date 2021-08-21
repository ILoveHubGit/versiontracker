(ns versiontracker.forms.core
  (:require [re-frame.core :as rf]))

(def root-db-path [::forms])
(def value-db-path (conj root-db-path ::value))

(rf/reg-sub
 ::values
 (fn [db]
   (get-in db value-db-path)))

(rf/reg-sub
 ::field-value
 :<- [::values]
 (fn [forms-data [_ form-id field-path]]
   (get-in forms-data (vec (cons form-id field-path)))))

(rf/reg-event-db
 ::set-text-field-value
 (fn [db [_ form-id field-path new-value]]
   (assoc-in db (vec (concat value-db-path (cons form-id field-path))) (when (seq new-value) new-value))))

(def formatter (.NumberFormat. js/Intl "en-US" (js-obj {"style" "currency" "currency" "USD" "maximumFractionDigits" 1})))
(defn format [num]
  (if (= num "") "" (.format formatter num)))

(rf/reg-event-db
 ::set-number-field-value
 (fn [db [_ form-id field-path new-value type]]
  (let [number-value (when (seq new-value)
                       (case type
                             :int (js/parseInt new-value)
                             :float (js/parseFloat new-value) nil))]
     (assoc-in db (vec (concat value-db-path (cons form-id field-path))) number-value))))

(rf/reg-event-db
 ::set-dropdown-value
 (fn [db [_ form-id field-path field-store new-value e-key list]]
   (assoc-in db (vec (concat value-db-path (cons form-id field-store)))
             (filter #(= (str (:id %)) new-value) list))))
