(ns cljem.mode.default
  (:require cljem.modef)
  (:use [cljem mdi]))

;; the default mode which is the parent of all other modes

(defn close-active-frame
  "Close the active frame"
  {:interactive true}
  []
  "TBD")

(cljem.modef/init-mode
 :display-name "default"
 :keymap {"D" #'close-active-frame}
 :widget (desktop-pane :id :desktop
                       :background "gray"))
