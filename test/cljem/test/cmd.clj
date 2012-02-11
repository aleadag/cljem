(ns cljem.test.cmd
  (:use cljem.cmd
        lazytest.describe))

(defn dummy-cmd
  "This is a dummy cmd for testing purpose"
  {:interactive true}
  [
   #^{:from :event}
   arg0
   #^{:from :user-input
      :prompt "Please enter: "
      :default [1 1]
      :completion-fn #(let [_ %] [[1 1] [1 0]])}
   arg1
   #^{:from :user-selection
      :prompt "Please select a value: "
      :completion-fn #(nth [1 2 3 4 5] %)}
   arg2]
  :dummy)

(describe resolve-cmd
  (it "can resolve a cmd"
    (= #'dummy-cmd
       (resolve-cmd "dummy-cmd")))

  ;; (it "cannot resolve a non-exist cmd"
  ;;   (nil? (resolve-cmd "i-donnot-exist")))

  (it "can resolve cmd by var name"
    (= #'dummy-cmd
       (resolve-cmd "#'cljem.test.cmd/dummy-cmd"))))

  ;; (it "cannot resolve a non-exist cmd"
  ;;   (nil? (resolve-cmd "#'dummy-cmd")))

(describe get-arg-meta
  (it "can return the fn arg meta"
    (contains? (get-arg-meta #'dummy-cmd 0)
               :from)))

(describe user-interaction-arg?
  (it "can deterine whether an arg needs user input"
    (user-interaction-arg? (get-arg-sym #'dummy-cmd 1)))
  (it "can determine when an arg need not user input"
    (not (user-interaction-arg? (get-arg-sym #'dummy-cmd 0)))))

(describe prepare-arg
  (given [e (java.awt.event.ActionEvent. "source" 1 "dummy-command")]
    (it "can get arglist by arg tag"
      (= e
         (prepare-arg (first (first (:arglists (meta #'dummy-cmd))))
                      e)))
    (it "cannot get arglist without the correct args"
      (= nil
         (prepare-arg (first (first (:arglists (meta #'dummy-cmd))))
                      "I am not an event")))
    (it "can prepare arg by selection"
      (= 4
         (prepare-arg (get-arg-sym #'dummy-cmd 2)
                      "  3 ")))
    (it "can prepare non-string arg"
      (= [1 0]
         (prepare-arg (get-arg-sym #'dummy-cmd 1)
                      " [1 0] ")))))

(describe args-ready?
  (it "can determine when a cmd has full args"
    (args-ready? {:f #'dummy-cmd
                  :arglist [1 2 3]}))
  (it "can tell when the arglist is not ready"
    (not (args-ready? {:f #'dummy-cmd
                       :arglist [1]}))))

(describe all-cmds
  (it "can get all available cmds"
    (not (apply distinct?
                #'dummy-cmd
                (all-cmds)))))

(describe gen-cmd-completion
  (it "can return the completion of a cmd"
    (not (apply distinct?
                #'dummy-cmd
                (gen-cmd-completion "dum"))))
  (it "cannot return if the completion is incorrect"
    (apply distinct?
           #'dummy-cmd
           (gen-cmd-completion "i-donnot-exist"))))

(describe gen-arg-completion
  (it "can complete an arg"
    (= [[1 1] [1 0]]
       (gen-arg-completion (get-arg-sym #'dummy-cmd 1)
                           "hint"))))

(describe eval-cmd
  (it "can evaluate the cmd fn"
    (= :dummy
       (eval-cmd {:f #'dummy-cmd
                  :arglist [1 2 3]}))))
