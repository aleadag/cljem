(ns cljem.cmd
  (:use [clojure.string :only [trim]]
        [seesaw.util :only [illegal-argument]]))

;; :f function
;; :arglist arg-value-list
(def current-cmd-data (ref {}))

(defn cmd? [f]
  (:interactive (meta f)))

(defn- resolve-cmd-by-name [name]
  {:pre [(string? name)]}
  (let [sym (symbol name)
        cmd-vars  (filter #(and % (cmd? %))
                          (keep #(ns-resolve % sym)
                                (all-ns)))]
    (or (first cmd-vars)
        (illegal-argument "Invalid command name: %s" name))))

(defn- resolve-cmd-by-var [v-name]
  {:pre [(string? v-name) (.startsWith v-name "#'")]}
  (eval (read-string v-name)))

(defn resolve-cmd [s]
  {:pre [(string? s)]}
  (if (.startsWith s "#'")
    (resolve-cmd-by-var s)
    (resolve-cmd-by-name s)))

(defn get-arg-sym [f n]
  (nth (first (:arglists (meta f))) n))

(defn get-arg-meta [f n]
  (meta (get-arg-sym f n)))

(defn user-interaction-arg? [arg-sym]
  (contains? #{:user-input :user-selection}
             (:from (meta arg-sym))))

(defmulti prepare-arg (fn [arg-sym & args]
                        (:from (meta arg-sym))))

(defmethod prepare-arg :event
  [arg-sym & args]
  (first (filter #(instance? java.util.EventObject %)
                 args)))

(defmethod prepare-arg :user-input
  [arg-sym & args]
  {:pre [(user-interaction-arg? arg-sym)
         (= 1 (count args))]}
  (let [text (trim (first args))]
    (if (string? (:default (meta arg-sym)))
      text
      (read-string text))))

(defmethod prepare-arg :user-selection
  [arg-sym & args]
  {:pre [(user-interaction-arg? arg-sym)
         (= 1 (count args))]}
  (let [comp-fn (:completion-fn (meta arg-sym))
        k (read-string (first args))]
    (if comp-fn (apply (eval comp-fn) [k]) nil)))

(defn reset-cmd! []
  (dosync
   (ref-set current-cmd-data {})))

(defn get-cmd-entered
  ([cmd-data]
     (:f cmd-data))
  ([]
     (get-cmd-entered @current-cmd-data)))

(defn cmd-entered?
  ([cmd-data]
     (not (nil? (get-cmd-entered))))
  ([]
     (cmd-entered? @current-cmd-data)))

(defn args-ready?
  ([cmd-data]
     (= (count (:arglist cmd-data))
        (count (first (:arglists (meta (:f cmd-data)))))))
  ([]
     (args-ready? @current-cmd-data)))

(defn set-cmd-fn! [f]
  (dosync (alter current-cmd-data
                 assoc
                 :f f))
  f)

(defn add-cmd-args! [& args]
  (dosync (alter current-cmd-data
                 assoc
                 :arglist (concat (:arglist @current-cmd-data)
                                  args))))

(defn get-next-arg-index
  ([cmd-data]
     (count (:arglist cmd-data)))
  ([]
     (get-next-arg-index @current-cmd-data)))

(defn get-next-arg-sym
  ([cmd-data]
     (if (args-ready? cmd-data)
       nil
       (get-arg-sym (:f cmd-data)
                    (get-next-arg-index cmd-data))))
  ([]
     (get-next-arg-sym @current-cmd-data)))

(defn prepare-args-until-user-interaction!
  [& args]
  (loop [arg-sym (get-next-arg-sym)]
    (when (and arg-sym
               (not (user-interaction-arg? arg-sym)))
      (add-cmd-args! (apply prepare-arg arg-sym args))
      (recur (get-next-arg-sym)))))

(defn enter-arg [text]
  (let [arg-sym (get-arg-sym (get-cmd-entered)
                             (get-next-arg-index))]
    (add-cmd-args! (prepare-arg arg-sym text))))

(defn enter-cmd [text]
  {:post [(cmd-entered?)]}
  (let [cmd (resolve-cmd text)]
    (if cmd
      (set-cmd-fn! cmd)
      nil)))

(defn enter-cmd-info [text]
  (if-not (cmd-entered?)
    (enter-cmd text)
    (enter-arg text)))

(defn- ns-vars [ns]
  (map second (ns-publics ns)))

(defn all-cmds []
  (filter cmd? (mapcat identity
                       (map ns-vars (all-ns)))))

(def all-cmds-fast (memoize all-cmds))

(defn ns-cmds
  "Get the commands in a specified ns"
  [ns]
  {:pre [(the-ns ns)]}
  (filter cmd? (ns-vars (the-ns ns))))

(defn gen-cmd-completion [hint]
  (filter #(.startsWith (name (:name (meta %)))
                        hint)
          (all-cmds)))

(defn gen-arg-completion [arg-sym hint]
  (let [comp-fn (:completion-fn (meta arg-sym))]
    (if comp-fn
      (apply (eval comp-fn) [hint])
      nil)))

(defn gen-completion [hint]
  (if (cmd-entered?)
    (gen-arg-completion (get-next-arg-sym) hint)
    (gen-cmd-completion hint)))

(defn get-completion-title []
  (if (cmd-entered?)
    (let [arg-sym (get-next-arg-sym)]
      (or (:prompt (meta arg-sym)) name))
    "*Completion*"))

(defn get-arg-completion-opts [arg-sym]
  (if-let [opts (:completion-opts (meta arg-sym))]
    (eval opts)))

(defn get-completion-opts []
  (if (cmd-entered?)
    (get-arg-completion-opts (get-next-arg-sym))))

(defn eval-cmd
  ([cmd-data]
     {:pre [(get-cmd-entered cmd-data)
            (args-ready? cmd-data)]}
     (apply (var-get (:f cmd-data))
            (:arglist cmd-data)))
  ([]
     (eval-cmd @current-cmd-data)))
