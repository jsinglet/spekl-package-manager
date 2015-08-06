(ns spekl-package-manager.check
 (:require [clojure.java.io :as io]
            [clojure.tools.logging :as log]))

;;
;; All checks must at least define a "default" check and can optionally define any number of named checks.
;;
(defcheck default
  ;; Printing a log message...
  (log/info  "A message")

  ;; running a check
  (log/info  "Running a check.")  
  (run "cmd" "arg1" "${"$"}{installed-package:asset.jar}" "-esc" *project-files-string* ))

