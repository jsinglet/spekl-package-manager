(ns spekl-package-manager.constants
  (:require [spekl-package-manager.runtime :as rt])
  )

(defn project-filename [] (str (rt/working-dir) "spekl.yml"))
(defn package-filename [] (str (rt/working-dir) "package.yml"))
(defn package-directory [] (str (rt/working-dir) ".spm"))


(def org-name "Spekl")

(def spm-home "http://api.spekl-project.org/")
(defn spm-package-list [] (str spm-home "static-package-list.json"))
(defn git-base [] "https://github.com/Spekl/")

(def api-raw "https://raw.githubusercontent.com/")
(def api-all-repos (str "https://api.github.com/orgs/" org-name "/repos"))
(def api-all-teams (str "https://api.github.com/orgs/" org-name "/teams"))

(defn api-delete-repo [repo] (str "https://api.github.com/repos/" org-name "/" repo))
(defn api-delete-team [team] (str "https://api.github.com/teams/" team))



(def api-key "04faf84ae7b17fbc0e5cd9f17b78afe338f717a3")
(def api-name "spekl-admin")

(defn check-file [] "check.clj")
