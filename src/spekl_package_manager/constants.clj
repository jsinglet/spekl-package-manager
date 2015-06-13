(ns spekl-package-manager.constants
  (:require [spekl-package-manager.runtime :as rt])
  )

(defn project-filename [] (str (rt/working-dir) "spekl.yml"))
(defn package-filename [] (str (rt/working-dir) "package.yml"))
(defn package-directory [] (str (rt/working-dir) ".spekl"))

