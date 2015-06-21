(ns spekl-package-manager.progress
  (:require [spekl-package-manager.download :as download])
  )


(defn silly-loop2 []
  (download/download-to "openjml-1.1.1" "http://jmlspecs.sourceforge.net/openjml.zip" "test.zip")
  )
