(ns versiontracker.forms.controls
  (:require [re-frame.core :as rf]
            [versiontracker.forms.core :as forms]
            [cljs.spec.alpha :as s]))

(defn text-input
  [form-id field-path field-spec validation-error-msg
   {:keys [label field-classes] :as options}]
  (let [field-value @(rf/subscribe [::forms/field-value form-id field-path])
        field-valid? (s/valid? field-spec field-value)]

    [:div.field
     {:class (cond->>  (or field-classes (list))
                 (and (not (nil? field-value)) (not field-valid?)) (cons "error")
                 true (clojure.string/join \space))}
     (when label [:label.label label])
     [:div.md-form
      [:input.input
       (if (some #(= "disabled" %) field-classes)
         {:class (cond->> (or field-classes (list)))
          :type      "text"
          :value     field-value
          :disabled  field-classes
          :on-change #(rf/dispatch [::forms/set-text-field-value form-id field-path (-> % .-target .-value)])}
         {:class (cond->> (or field-classes (list)))
          :type      "text"
          :value     field-value
          :on-change #(rf/dispatch [::forms/set-text-field-value form-id field-path (-> % .-target .-value)])})]
      (when-not (and (not (nil? field-value)) field-valid? [:div.error validation-error-msg]))]]))

(defn number-input
  [form-id field-path field-spec validation-error-msg
   {:keys [label field-classes] :as options} type]
  (let [field-value   @(rf/subscribe [::forms/field-value form-id field-path])
        field-valid? (s/valid? field-spec
                      (case type
                              :int (if (= (js/parseInt field-value) (js/parseFloat field-value)) (js/parseInt field-value) field-value)
                              :float (js/parseFloat field-value)
                              0))]
    [:div.field
     {:class (cond->>  (or field-classes (list))
                 (not field-valid?) (cons "error")
                 true (clojure.string/join \space))}
     [:div.box
      (when label [:b label])
      [:div.md-form
       [:input.input
        {:type      "text"
         :value     field-value
         :on-change #(rf/dispatch [::forms/set-number-field-value form-id field-path (-> % .-target .-value) type])}]]
      (when-not field-valid? [:div.error validation-error-msg])]]))

(defn dropdown-list
  "Returns an <OPTION ...> line for a dropdown box"
  [e-key e-value list-item]
  [:option {:key (rand) :value (e-key list-item)} (e-value list-item)])

(defn dropdown
  "Creates a dropdown box for a list
   | Key           | Description |
   | ------------- | ----------- |
   | form-id       | Name of the form used
   | field-path    | Name of the field in the form
   | list          | A map with key/value pairs to fill a dropdown box
   | field-classes | Custom css classes to be added to the form field
   | label         | The GUI representation of the field"
  [form-id field-path field-store list e-key e-value
   {:keys [label field-classes] :as options}]
  [:div.field
   (when label [:label.label label])
   [:div.field-body
    [:div.field
     [:div.select
      {:class (cond->> (or field-classes (list)))}
      [:select
       {:on-change #(rf/dispatch [::forms/set-dropdown-value form-id field-path field-store (-> % .-target .-value) e-key list])}
       (map #(dropdown-list e-key e-value %) list)]]]]])
