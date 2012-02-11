(ns cljem.core
  (:use cljem.main-frame
        [seesaw core]))

(defn -main [& args]
  (require 'cljem.init)
  (native!)
  (invoke-later
   (->
    (make-main-frame)
    (config! :title "Testing ...")
    show!)))
