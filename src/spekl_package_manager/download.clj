(ns spekl-package-manager.download
  (:require [clj-http.client :as client]))


( (client/get "http://jmlspecs.sourceforge.net/openjml.zip") :headers)


;;((client/get "http://www.google.com") :headers)
