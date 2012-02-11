(ns cljem.modef
  (:require [clojure.string :as string])
  (:use [seesaw core keymap keystroke]
        [clojure.tools.logging :only [debug]]
        [cljem cmd minibuffer win-util]))

(def all-mode-metadata (ref {}))

(defn init-mode-metadata [mode-name]
  (dosync (commute all-mode-metadata
                   assoc
                   mode-name
                   (ref {}))))

(defn mode-metadata-for
  ([mode-name]
     (@all-mode-metadata mode-name))
  ([mode-name category]
     (@(mode-metadata-for mode-name) category)))

(defn set-mode-metadata-for [mode-name category value]
  (let [meta-ref (mode-metadata-for mode-name)]
    (ref-set meta-ref (assoc @meta-ref category value))))

(defn display-name-for [mode-name]
  (mode-metadata-for mode-name :display-name))

(defn set-display-name-for [mode-name display-name]
  (dosync (set-mode-metadata-for mode-name
                                 :display-name
                                 display-name)))

(defn widget-for [mode-name]
  (mode-metadata-for mode-name :widget))

(defn set-widget-for [mode-name widget]
  (dosync (set-mode-metadata-for mode-name
                                 :widget
                                 widget)))

(defn get-mode-by-widget [widget]
  (key (some #(== widget (:widget @(val %)))
             @all-mode-metadata)))

(defn- split-out-init-options [init-options]
  (loop [top-level-options {}
         remaining-options init-options]
    (if (keyword? (first remaining-options))
      (recur
       (assoc top-level-options (first remaining-options) (fnext remaining-options))
       (nnext remaining-options))
      [top-level-options remaining-options])))

(defn define-key-for [widget key cmd]
  {:pre [widget (var? cmd)]}
  (debug (format "Binding key for widget(id = %s): %s => %s"
                 (config widget :id)
                 key
                 cmd))
  (map-key widget key (fn [e]
                        (handle-user-input #(set-cmd-fn! cmd)
                                           e))))

(defn add-widget [^javax.swing.JDesktopPane dt
                  ^javax.swing.JInternalFrame w]
  {:pre [dt w]}
  (.add dt w)
  (doto w
    .pack
    (config! :maximum true)))

(defn desktop-pane []
  (widget-for "default"))

(defn active-widget [^javax.swing.JDesktopPane dt
                     ^javax.swing.JInternalFrame w]
  {:pre [dt w]}
  (.setSelectedFrame dt w))

(defmacro init-mode
  [& init-options]
  (let [mode-name (last (string/split (name (ns-name *ns*)) #"\."))
        [top-level-options option-groups] (split-out-init-options init-options)
        display-name (:display-name top-level-options)
        keymap (:keymap top-level-options)
        widget (:widget top-level-options)]
    `(do
       (init-mode-metadata ~mode-name)
       (set-display-name-for ~mode-name ~display-name)
       ;; Please be note that: ~widget and (widget-for ~mode-name) is different
       ;; Everytime call ~widget, it will create a new widget
       (def ~'mode-name ~mode-name)
       (defn ~'create-widget []
         (let [w# ~widget]
           (set-widget-for ~mode-name w#)
           (doseq [[k# v#] (map identity ~keymap)]
             (define-key-for w# k# v#))
           (doseq [cmd# (ns-cmds ~*ns*)]
             (if-let [k# (:keymap (meta cmd#))]
               (define-key-for w# k# cmd#)))
           w#))
       (when (not= ~mode-name "default")
         (defn ~(symbol (str mode-name))
           {:interactive true}
           []
           (if (widget-for ~mode-name)
             (active-widget (desktop-pane)
                            (widget-for ~mode-name))
             (let [w# (~'create-widget)]
               (add-widget (desktop-pane) w#)))
           (str "Loaded " ~mode-name)))
       (defn ~'display-name [] (display-name-for ~mode-name))
       (defn ~'widget [] (widget-for ~mode-name))
       (defn ~'define-key [key# cmd#]
         {:pre [(widget-for ~mode-name)]}
         (define-key-for (widget-for ~mode-name) key# cmd#)))))
