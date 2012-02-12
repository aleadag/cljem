(ns cljem.init)

(defn my-dummy-cmd
  "This is a dummy cmd for testing purpose"
  {:interactive true}
  [#^{:from :event} e
   #^{:from :user-input
      :prompt "Please enter an array: "
      :default [1 1]
      :completion-fn (fn [input e] (prn e) [[1 1] [1 0]])}
   fx]
  fx)
