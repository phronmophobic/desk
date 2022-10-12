(ns com.phronemophobic.desk.playground
  (:require [membrane.ui :as ui]
            [membrane.component :as component
             :refer [defui defeffect]]
            [membrane.basic-components :as basic]
            [com.phronemophobic.desk :as desk]))

;; This is a markdown block
;; with multiple
;; lines of text

(declare state)

(defn add-viewer! [viewer]
  (swap! state update :viewers desk/add-viewer viewer))

(comment
  ;; open window
  (defonce state (desk/show!))

  ;; quit watching this file.
  (desk/unwatch! state)

  ,)

(defui string-viewer [{:keys [obj]}]
  (ui/label (str obj ": " (count obj))))

(def this-is-a "this is a ")



(defui boolean-viewer [{:keys [obj]}]
  (ui/checkbox obj))


true


(comment
  (add-viewer! {:pred boolean?
                :id :boolean?
                :render-fn boolean-viewer})
  ,)

[1 2 3 4 5 6 7]


(defn my-view []
  (ui/label "hi there"))



{::ui-viewer (ui/label "Hello World!")}

(comment
  (add-viewer! {:pred ::ui-viewer
                :render-fn (fn [{:keys [obj]}]
                             (::ui-viewer obj))})


  ,)

(defui buttons! [{:keys [obj]}]
  (apply
   ui/vertical-layout
   (for [{:keys [name action]} (::name-actions obj)]
     (basic/button {:text name
                    :on-click
                    (fn []
                      (action)
                      nil)}))))

{::name-actions
 [{:name "print foo"
   :action (fn [] (println "foo"))}
  {:name "print bar"
   :action (fn [] (println "bar"))}]}

{::name-actions
 (for [num (range 10)]
   {:name (str "name " num)
    :action (fn [] (prn num))})}

(comment
  (add-viewer! {:pred ::name-actions
                :id ::name-actions
                :render-fn buttons!})
  ,)
