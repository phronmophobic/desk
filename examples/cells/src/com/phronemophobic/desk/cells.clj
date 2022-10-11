(ns com.phronemophobic.desk.cells)


(defui vertical-block-viewer [{:keys [obj]}]
  (let [viewerf (:viewerf context)]
    (apply
     ui/vertical-layout
     (for [child obj
           :let [viewer (viewerf child)
                 block-extra (get extra [:extra $child])]]
       (when-let [render-fn (:render-fn viewer)]
         (render-fn {:obj child
                     :extra block-extra
                     :$extra $block-extra
                     :context (assoc context
                                     :viewerf viewerf)
                     :$context $context}))))))

(defui horizontal-block-viewer [{:keys [obj]}]
  (let [viewerf (:viewerf context)]
    (apply
     ui/horizontal-layout
     (for [child obj
           :let [viewer (viewerf child)
                 block-extra (get extra [:extra $child])]]
       (when-let [render-fn (:render-fn viewer)]
         (render-fn {:obj child
                     :extra block-extra
                     :$extra $block-extra
                     :context (assoc context
                                     :viewerf viewerf)
                     :$context $context}))))))

(defui block-viewer [{:keys [obj]}]
  (ui/padding 1
   (if (pos? obj)
     (ui/with-color [0 0 0]
       (ui/with-style ::ui/style-stroke-and-fill
         (ui/rectangle 10 10)))
    
     (ui/with-color [0 0 0]
       (ui/with-style ::ui/style-stroke
         (ui/rectangle 10 10))))))

(defui map-block-viewer [{:keys [obj]}]
  (let [viewerf (:viewerf context)]
    (apply
     ui/vertical-layout
     (for [row obj
           
           ]
       (apply
        ui/horizontal-layout
        (interpose
         (ui/spacer 10)
         (for [child row
               :let [viewer (viewerf child)
                     block-extra (get extra [:extra $child])]]
           (when-let [render-fn (:render-fn viewer)]
             (render-fn {:obj child
                         :extra block-extra
                         :$extra $block-extra
                         :context (assoc context
                                         :viewerf viewerf)
                         :$context $context})))))
       ))))


(s/def ::block number?)

(s/def ::horizontal-blocks
  (every-pred vector? (partial every? (some-fn number? vector?))))

(s/def ::vertical-blocks
  (every-pred list? (partial every? (some-fn number? vector?))))

(s/def ::block-group
  (s/or :horizontal ::horizontal-blocks
        :vertical ::vertical-blocks
        :single ::block))

(s/def ::block-map
  (s/every-kv ::block-group ::block-group))


(comment
  (add-viewer {:pred string?
               :id ::string
               :render-fn string-viewer})


  (add-viewer {:pred #(and (map? %)
                           (contains? % :nextjournal.clerk/var-from-def))
               :id ::var-viewer
               :render-fn var-viewer})

  (add-viewer {:pred number?
               :id ::number-block
               :render-fn block-viewer})



  (add-viewer {:pred (every-pred list? (partial every? (some-fn number? vector?)))
               :id ::vertical-block
               :render-fn vertical-block-viewer})

  (add-viewer {:pred (every-pred vector? (partial every? (some-fn number? vector?)))
               :id ::horizontal
               :render-fn horizontal-block-viewer})

  (add-viewer {:pred #(s/valid? ::block-map %)
               :id ::block-map
               :render-fn map-block-viewer})

  

  ,
  )


(def foo 41)

(def foo42 42)

'(0 1 0 0 1)

(mapv #(- % 5) (range 10))

-1

(def rule-30
  {[1 1 1] 0
   [1 1 0] 0
   [1 0 1] 0
   [1 0 0] 1
   [0 1 1] 1
   [0 1 0] 1
   [0 0 1] 1
   [0 0 0] 0})


rule-30


(def first-generation
  (let [n 33]
    (assoc (vec (repeat n 0)) (/ (dec n) 2) 1)))

(def board
  (let [evolve #(mapv rule-30 (partition 3 1 (repeat 0) (cons 0 %)))]
    (->> first-generation (iterate evolve) (take 17) (apply list))))

(s/def ::color-value
  (s/or :zero-or-one #{0 1}
        :double (s/double-in :min 0 :max 1)))
(s/def ::color (s/every number? :min-count 3 :max-count 4))
(comment
  (add-viewer {:pred #(s/valid? ::color %)
               :render-fn (fn [{:keys [obj]}]
                            (ui/filled-rectangle obj 30 30))})
  ,
  
  )

