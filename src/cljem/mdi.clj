(ns cljem.mdi
  (:use seesaw.core
        [seesaw.util :only [resource]]
        [seesaw.options :only [ignore-option default-option bean-option 
                               resource-option around-option
                               apply-options 
                               option-map option-provider
                               get-option-value]]
        [seesaw.widget-options :only [widget-option-provider]]))

(def desktop-pane-options default-options)

(widget-option-provider javax.swing.JDesktopPane desktop-pane-options)

(defn desktop-pane
  [& opts]
  (apply-options (construct javax.swing.JDesktopPane) opts))

(def internal-frame-options
  (merge default-options
         (option-map
          (resource-option :resource [:title :icon])

          (default-option 
            :content
            (fn [^javax.swing.JInternalFrame f v] 
              (doto f
                (.setContentPane (make-widget v))
                .invalidate
                .validate
                .repaint))
            (fn [^javax.swing.JInternalFrame f] (.getContentPane f))
            "The frame's main content widget")
          (bean-option :resizable? javax.swing.JInternalFrame boolean)
          (bean-option :closable? javax.swing.JInternalFrame boolean)
          (bean-option :maximizable? javax.swing.JInternalFrame boolean)
          (bean-option :iconifiable? javax.swing.JInternalFrame boolean)
          (bean-option :maximum javax.swing.JInternalFrame boolean)
          (bean-option :title javax.swing.JInternalFrame resource nil
                       ["The frame's title as string or resource key"]))))

(widget-option-provider javax.swing.JInternalFrame internal-frame-options)

(defn internal-frame
  [& opts]
  (apply-options (construct javax.swing.JInternalFrame) opts))
