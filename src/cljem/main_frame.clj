(ns cljem.main-frame
  (:require [clojure.tools.logging :as logger]
            [cljem.minibuffer :as mb]
            [cljem.mode.default :as default-mode])
  (:use [seesaw core swingx keymap]
        [cljem minibuffer win-util]))

(defn make-completion-list []
  (defn on-selection-changed [e]
    (let [lb (to-widget e)
          root (to-root lb)
          mbc (select root [:#minibuffer])]
      (when (not= (.getSelectedIndex lb) -1)
        (mb/set-text! mbc (str (.getSelectedValue lb))))))
  (listbox-x :id :completion-list
             :listen [:selection on-selection-changed]))

(defn make-completion-panel []
  (let [cp (titled-panel :id :completion
                         :visible? false
                         :title-color "blue"
                         :content (make-completion-list))]
    (doto cp
      (map-key "Q" (fn [_] (hide! cp))))))

(defn make-cmd-frame []
  (border-panel :id :cmd-frame
                :north (make-completion-panel)
                :south (mb/make-minibuffer)))

(defn make-main-frame []
  (let [frm (frame :width 500 :height 500 
                   :on-close :exit
                   :content (border-panel :border 5 :hgap 5 :vgap 5
                                          :center (default-mode/create-widget)
                                          :south  (make-cmd-frame)))]
    (doto frm
      (map-key "alt X" #(mb/switch-mode-to (mb/get-minibuffer (to-root %)) :insert))
      (map-key "control G" mb/exit-command-mode)
      (map-key "control N" (fn [_] (focus-next)))
      (map-key "control P" (fn [_] (focus-previous))))))
