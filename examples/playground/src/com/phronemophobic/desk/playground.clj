(ns com.phronemophobic.desk.playground
  (:require [membrane.ui :as ui]
            [membrane.java2d :as backend]
            [membrane.component :as component
             :refer [defui defeffect]]
            [membrane.basic-components :as basic]
            [com.phronemophobic.desk :as desk]))

;; This is a markdown block
;; alsdkjfas
;; asdfas


(defonce state (atom {:viewers desk/default-viewers}))

(defn add-viewer! [viewer]
  (swap! state update :viewers desk/add-viewer viewer))


(defn show! []
  (desk/watch! state)
  (backend/run (component/make-app #'desk/doc-viewer state)
    {:window-title (str "Desk - " (ns-name *ns*))}))

(defui string-viewer [{:keys [obj]}]
  (ui/label (str obj ": " (count obj))))

(def app (component/make-app #'desk/doc-viewer state))

(def this-is-a "this is a ")

(def string-block (-> @state
     :doc
     :blocks
     (nth 5)))


(defui boolean-viewer [{:keys [obj]}]
  (ui/checkbox obj))


true

@state

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
