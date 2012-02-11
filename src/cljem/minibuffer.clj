(ns cljem.minibuffer
  (:import [javax.swing.text DocumentFilter AttributeSet]
           [java.awt.event KeyEvent])
  (:use [seesaw core meta keymap swingx]
        [clojure.repl :only [pst]]
        [clojure.tools.logging :only [debug]]
        [cljem cmd win-util]))

(defn get-mode [minibuffer]
  (get-meta minibuffer :mode))

(defn set-mode! [minibuffer mode]
  (put-meta! minibuffer :mode mode))

(defn get-minibuffer [root]
  (select root [:#minibuffer]))

(defn get-completion-pane [root]
  (select root [:#completion]))

(defn get-completion-listbox [root]
  (select root [:#completion-list]))

(defn hide-completion! [root]
  (if-let [comp-pane (get-completion-pane root)]
    (hide! comp-pane)))

(defn get-prompt-len [minibuffer]
  (or (get-meta minibuffer :prompt-len)
      0))

(defn set-prompt-len [minibuffer len]
  (put-meta! minibuffer :prompt-len len))

(defn set-text! [mb text]
  (let [prompt-len (get-prompt-len mb)]
    (set-prompt-len mb 0)
    (config! mb :text (str (subs (config mb :text)
                                 0
                                 prompt-len)
                           text))
    (set-prompt-len mb prompt-len)))

(defn switch-mode-to
  [minibuffer mode]
  (when-not (= mode (get-mode minibuffer))
    (case mode
      :normal (do
                (set-prompt-len minibuffer 0)
                (reset-cmd!)
                (if-let [root (to-root minibuffer)]
                  (hide-completion! root))
                (config! minibuffer
                         :editable? false
                         :text "Please enter \"ALT-X\" to eval a command"
                         :background "black"
                         :foreground "white")
                (if (.isFocusOwner minibuffer)
                  (.transferFocusBackward minibuffer)))
      :insert (do
                (config! minibuffer
                         :editable? true
                         :text ""
                         :background "white"
                         :foreground "black")
                (.requestFocusInWindow minibuffer)))
    (set-mode! minibuffer mode)))

(defn ask-for-input [minibuffer prompt default]
  (if (not= :insert (get-mode minibuffer))
    (switch-mode-to minibuffer :insert)
    (set-prompt-len minibuffer 0))
  (config! minibuffer
           :text (str prompt default))
  (.select minibuffer
           (count prompt)
           (+ (count prompt)
              (count default)))
  (set-prompt-len minibuffer (count prompt)))

(defn get-user-input [minibuffer]
  (subs (config minibuffer :text)
        (get-prompt-len minibuffer)))

(defn- editable? [minibuffer offset]
  (>= offset (get-prompt-len minibuffer)))

(defn- message [minibuffer msg bg fg]
  (switch-mode-to minibuffer :normal)
  (config! minibuffer
           :text msg
           :background bg
           :foreground fg))

(defn error [minibuffer msg]
  (message minibuffer msg "red" "white"))

(defn info [minibuffer msg]
  (message minibuffer msg "green" "white"))

(defn default-completion-render-fn [renderer info]
  #_(let [index (:index info)]
    (apply config! renderer 
           (if (even? index) 
             [:background "white"]
             [:background "#f5f5f5"]))))

(def ^{:private true} default-completion-listbox-opts
  {:sort-order :ascending
   :highlighters [(hl-simple-striping :background "#f5f5f5")
                  ((hl-color :selected-background :yellow
                             :selected-foreground :black) :always)]})

(defn show-completion [root comp-title comp-list completion-opts]
  (let [comp-pane (get-completion-pane root)
        comp-listbox (get-completion-listbox root)
        listbox-opts (merge {:model comp-list
                             :renderer default-completion-render-fn}
                            default-completion-listbox-opts
                            completion-opts)]
    (config! comp-pane
             :title comp-title)
    (apply config! comp-listbox
           (interleave (keys listbox-opts)
                       (vals listbox-opts)))
    (.setSelectedIndex comp-listbox 0)
    (show! comp-pane)))

(defn start-next-arg [mb e]
  (let [arg-sym (get-next-arg-sym)
        arg-info (meta arg-sym)]
    (debug arg-info)
    (when arg-info
      (switch-mode-to mb :insert)
      (ask-for-input mb
                     (or  (:prompt arg-info)
                          (name arg-sym))
                     (str (or (:default arg-info)
                              ""))))))

(defn on-user-tab [text mb e]
  (let [cl (gen-completion text)]
    (cond
      (nil? cl) nil
      (= (count cl) 1) (set-text! mb (first cl))
      (> (count cl) 1) (show-completion (to-root e)
                                        (get-completion-title)
                                        cl
                                        (get-completion-opts)))))

(defn handle-user-input [input-fn e]
  (let [root (to-root e)
        mb (get-minibuffer root)
        comp-pane (get-completion-pane root)]
    (hide! comp-pane)
    (try
      (input-fn)
      (prepare-args-until-user-interaction! e)
      (if-not (args-ready?)
        (start-next-arg mb e)
        (do
          (info mb (str (eval-cmd)))
          (reset-cmd!)))
      (catch Exception e
        (error mb (.getMessage e))
        (pst e 100)
        (reset-cmd!)))))

(def ^{:private true} keys-to-completion-list
  #{KeyEvent/VK_DOWN
    KeyEvent/VK_UP
    KeyEvent/VK_PAGE_DOWN
    KeyEvent/VK_PAGE_UP})

(defn on-key-event [e]
  (let [root (to-root e)
        comp-pane (get-completion-pane root)
        comp-listbox (get-completion-listbox root)]
    (when (and (config comp-pane :visible?)
               (keys-to-completion-list (.getKeyCode e)))
      (.setSource e comp-listbox)
      (.dispatchEvent comp-listbox e))))

(defn exit-command-mode [e]
  (let [root (to-root e)
        mb (get-minibuffer root)]
    (when (= :insert (get-mode mb))
      (reset-cmd!)
      (switch-mode-to mb :normal))))

(defn make-minibuffer []
  (let [mb (text :id :minibuffer)]
    (doto mb
      (switch-mode-to :normal)
      (.. (getDocument) (setDocumentFilter (proxy [DocumentFilter] []
                                             (insertString [^javax.swing.text.DocumentFilter$FilterBypass fb
                                                            ^Integer offset
                                                            ^String text
                                                            ^AttributeSet attr]
                                               (if (editable? mb offset)
                                                 (proxy-super insertString fb offset text attr)))
                                             (remove [^javax.swing.text.DocumentFilter$FilterBypass fb
                                                      ^Integer offset
                                                      ^Integer length]
                                               (if (editable? mb offset)
                                                 (proxy-super remove fb offset length)))
                                             (replace [^javax.swing.text.DocumentFilter$FilterBypass fb
                                                       ^Integer offset
                                                       ^Integer length
                                                       ^String text
                                                       ^AttributeSet attr]
                                               (if (editable? mb offset)
                                                 (proxy-super replace fb offset length text attr))))))
      (listen :key-pressed on-key-event)
      (.setFocusTraversalKeysEnabled false)
      (map-key "ENTER" (fn [e]
                         (when (= :insert (get-mode mb))
                           (handle-user-input #(enter-cmd-info
                                                (get-user-input mb))
                                              e))))
      (map-key "TAB" #(when (= :insert (get-mode mb))
                        (on-user-tab (get-user-input mb) mb %)))
      (map-key "ESCAPE" exit-command-mode))))
