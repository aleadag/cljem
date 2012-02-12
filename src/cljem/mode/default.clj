(ns cljem.mode.default
  (:require cljem.modef)
  (:use [cljem mdi]
        [seesaw core]))

;; the default mode which is the parent of all other modes

(defn close-active-frame
  "Close the active frame"
  {:interactive true}
  [^{:from :event} e]
  (assert (instance? javax.swing.JDesktopPane (to-widget e)))
  (if-let [frm (.getSelectedFrame (to-widget e))]
    (do
      (.dispose frm)
      "DONE")
    "No active frame!"))

(cljem.modef/init-mode
 :display-name "default"
 :keymap {"D" #'close-active-frame}
 :widget (desktop-pane :id :desktop
                       :focusable? true
                       :background "gray"))
