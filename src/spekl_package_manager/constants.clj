(ns spekl-package-manager.constants
  (:require [spekl-package-manager.runtime :as rt])
  )

(defn project-filename [] (str (rt/working-dir) "spekl.yml"))
(defn package-filename [] (str (rt/working-dir) "package.yml"))
(defn package-directory [] (str (rt/working-dir) ".spm"))

(def org-name "Spekl")

(defn git-base [] "https://github.com/Spekl/")

(def api-raw "https://raw.githubusercontent.com/")
(def api-all-repos (str "https://api.github.com/orgs/" org-name "/repos"))
(def api-key "ee72820ba97e94c64819ae30b467c680af225273")
(def api-name "jsinglet")
