(ns cljem.win-util
  (:import [javax.swing FocusManager KeyStroke]
           [java.util Date]
           [java.awt Component]
           [java.awt.event KeyEvent])
  (:use [seesaw core keystroke]))

;; Focus related stuff
(defn focus-owner []
  (.. (javax.swing.FocusManager/getCurrentManager) (.getFocusOwner)))

(defn focus-next []
  (.focusNextComponent (FocusManager/getCurrentManager)))

(defn focus-previous []
  (.focusPreviousComponent (FocusManager/getCurrentManager)))

(defn gen-key-event [^Component src
                     ^KeyStroke ks]
  (KeyEvent. src
             (rand-int 1000000)
             (.. (Date.) (getTime))
             (.getModifiers ks)
             (.getKeyCode ks)
             (.getKeyChar ks)))
