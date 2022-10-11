(ns com.phronemophobic.desk
  (:require [nextjournal.clerk :as clerk]
            [nextjournal.clerk.eval :as eval]
            [clojure.string :as str]
            [babashka.fs :as fs]
            [membrane.ui :as ui]
            [clojure.java.io :as io]
            [clojure.edn :as edn]
            [membrane.components.code-editor.code-editor :as code-editor]
            [liq.buffer :as buffer]
            [membrane.component :as component
             :refer [defui defeffect]]
            [membrane.basic-components :as basic]
            [com.phronemophobic.viscous :as viscous]
            [nextjournal.beholder :as beholder])
  )



(defui obj-inspector [{:keys [obj]}]
  (viscous/inspector {:obj (viscous/wrap obj)
                      :extra extra
                      :show-context? false
                      :width (get extra ::obj-inspector-width 40)
                      :height (get extra ::obj-inspector-height 1)}))


(defui var-viewer [{:keys [obj]}]
  (let [viewerf (:viewerf context)
        obj (-> obj
                :nextjournal.clerk/var-from-def
                deref)
        viewer (viewerf obj)
        block-extra (get extra :block-extra)]
    (when-let [render-fn (:render-fn viewer)]
      (render-fn {:obj obj
                  :extra block-extra
                  :$extra $block-extra
                  :context (assoc context
                                  :viewerf viewerf)
                  :$context $context}))))

(defn markdown->view [md]
  (when (and (:text md)
             (:content md)
             (#{:softbreak} (:type md)))
    (throw (ex-info "Expecting at most :text or :content"
                    {:markdown md})))
  (cond

    (#{:softbreak} (:type md))
    nil

    (:text md)
    (ui/label (:text md))

    (:content md)
    (apply ui/vertical-layout
           (map markdown->view (:content md)))

    :else
    (throw (ex-info "Expecting either :text or :content"
                    {:markdown md}))))


(defui markdown-viewer [{:keys [block]}]
  (markdown->view (:doc block)))

(defui hide-viewer [{}]
  nil)


(defn with-background [color body]
  (let [[w h] (ui/bounds body)]
    [(ui/with-style ::ui/style-fill
       (ui/with-color color
         (ui/rectangle w h)))
     body]))


(def gray (repeat 3 0.8))

(defui source-code-viewer [{:keys [block]}]
  (let [form (:form block)
        buttons
        (when (= 'comment
                 (and
                  (seq? form)
                  (first form)))
          (into []
                (keep (fn [form]
                        (when-let [{:keys [end-row end-col]} (meta form)]
                          (ui/translate
                           (+ 4 (* code-editor/lw (dec end-col)))
                           (+ -6(* code-editor/lh (dec end-row)))
                           (basic/button {:text "eval"
                                          :on-click
                                          (fn []
                                            (binding [*ns* (:ns block)]
                                              (eval form))
                                            nil)
                                          :hover (get extra [:action-hover
                                                             [end-row end-col]])}))))
                      )
                (rest form)))]
    (with-background (repeat 3 0.96)
      (ui/padding
       8
       [buttons
        (ui/no-events
         (code-editor/text-editor {:buf
                                   (-> (buffer/buffer (:text block))
                                       (code-editor/highlight)
                                       (dissoc :liq.buffer/cursor))}))]))))

(defui code-viewer [{:keys [viewerf block]}]
  ;; (prn "code viewer " (:id block))
  (let [visibility (:visibility block)]
    (ui/vertical-layout
     (when (= :show (-> block :visibility :code))
       (source-code-viewer {:block block}))
     (when (= :show (:result visibility))
       (when-let [obj (-> block
                          :result
                          :nextjournal/value)]
         (let [block-extra (get extra [:extra (:id block)])
               viewer (viewerf obj)]
           (ui/vertical-layout
            #_(viscous/inspector {:obj (viscous/wrap viewer)
                                  :extra (get block-extra ::inspector)})
            (when-let [render-fn (:render-fn viewer)]
              (render-fn {:obj obj
                          :extra block-extra
                          :$extra $block-extra
                          :context (assoc context
                                          :viewerf viewerf)
                          :$context $context})))))))))

(defn best-viewer [viewers obj]
  (some (fn [viewer]
          (when-let [pred (:pred viewer)]
            (when (pred obj)
              viewer)))
        (reverse viewers)))

(defn viewers->viewerf [viewers]
  (memoize (partial best-viewer viewers)))

(def viewers->viewerf-memo (memoize viewers->viewerf))

(defui doc-viewer [{:keys [doc viewers]}]
  (let [viewerf (viewers->viewerf-memo viewers)]
    (basic/scrollview
     {:scroll-bounds [1000 800]
      :offset (get extra :doc-viewer/scroll-offset [0 0])
      :$body nil
      :body
      (apply
       ui/vertical-layout
       (when-let [title (:title doc)]
         (ui/label title (ui/font nil 42)))
       (for [block (:blocks doc)
             :let [block (assoc block
                                :ns (:ns doc))
                   block-extra (get extra [:extra (:id block)])
                   show-block? (get extra [:show-block? (:id block)])]
             ]
         (ui/padding
          8
          (ui/horizontal-layout
           #_#_(basic/checkbox {:checked? show-block?})
           (when show-block?
             (viscous/inspector {:obj (viscous/wrap block)
                                 :extra (get extra [:show-block (:id block)])} ))
           (cond
             (= :markdown (:type block))
             (markdown-viewer {:block block
                               :extra block-extra})

             (= :code (:type block))
             (code-viewer {:viewerf viewerf
                           :extra block-extra
                           :block block})))))
       )}))
  )

(defn add-viewer [viewers viewer]
  (if-let [i (some (fn [[i v]]
                     (when (= (:id viewer)
                              (:id v))
                       i))
                   (map-indexed vector viewers))]
    (assoc viewers i viewer)
    (conj viewers viewer)))


(def default-viewers [{:pred any?
                       :render-fn obj-inspector
                       :id :any?}
                      {:pred #(and (map? %)
                                   (contains? % :nextjournal.clerk/var-from-def))
                       :id ::var-viewer
                       :render-fn var-viewer}])


(def basis (-> (System/getProperty "clojure.basis") io/file slurp edn/read-string))

(defn ^:private ->ns-path [ns-or-fname]
  (cond

    (instance? clojure.lang.Namespace ns-or-fname)
    (->ns-path (ns-name ns-or-fname))

    (symbol? ns-or-fname)
    (let [parts (-> ns-or-fname
                    munge
                    name
                    (clojure.string/split #"\."))
          dir-parts (drop-last 1 parts)
          fname-part (last parts)
          
          fpath
          (first
           (for [suffix [".clj" ".cljc"]
                 path (:paths basis)
                 :let [file (apply io/file
                                   (concat [path]
                                           dir-parts
                                           [(str fname-part suffix)]))]
                 :when (.exists file)]
             (.getCanonicalPath file)))]

      (when-not fpath
        (throw (ex-info (str "Could not find ns for " ns-or-fname)
                        {:ns-or-fname ns-or-fname})))
      fpath)

    string?
    ns-or-fname

    :else (throw (ex-info (str "Could not find ns for " ns-or-fname)
                          {:ns-or-fname ns-or-fname}))))


(defn ^:private watch-callback
  [atm {:keys [type path] :as file-event}]
  (when (contains? #{:modify :create} type)
    (try
      (let [rel-path (str/replace (str path) (str (fs/canonicalize ".") fs/file-separator) "")
            _ (prn "updating doc")
            doc (time
                 (eval/eval-file rel-path))]
        
        (swap! atm
               assoc
               :doc doc))
      (catch Exception e
        (prn e))))
  )

(defn unwatch! [atm]
  ;; remove old
  (let [[{old-watcher :watcher} _new-state] (swap-vals! atm dissoc :watcher)]
    (when old-watcher
      (beholder/stop @old-watcher)))
  )

(defn watch!
  ([atm]
   (watch! atm *ns*))
  ([atm ns]
   (let [fpath (->ns-path ns)]
     (unwatch! atm)

     ;; try to add new
     (let [new-watcher (delay
                         (swap! atm assoc :doc (eval/eval-file fpath))
                         (beholder/watch (partial watch-callback atm)
                                         fpath))
           new-state
           (swap! atm update :watcher
                  (fn [watcher]
                    (if watcher
                      watcher
                      new-watcher)))]
       (if (identical? (:watcher new-state)
                       new-watcher)
         @new-watcher
         (do
           (prn "warning! adding watcher failed.")))))
   ;; return nil
   nil))


