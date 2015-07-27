(ns spekl-package-manager.danger
  (:require [clj-jgit.porcelain :as git]
            [clojure.java.io :as io]
            [clojure.tools.logging :as log]
            [spekl-package-manager.util :as util]
            [spekl-package-manager.constants :as constants]
            [spekl-package-manager.command-cache :as cache]
            [spekl-package-manager.package :as package]
            [clj-http.client :as client]
            [spekl-package-manager.net :as net]
            [clojure.data.json :as json]
            [clj-yaml.core :as yaml]           
            )
  )
 
(defn delete-repo [repo]
  (do
    (println (constants/api-delete-repo repo))
    (client/delete (constants/api-delete-repo repo)
                   {:basic-auth [constants/api-name constants/api-key]} 
                   )
))

(defn delete-all-repos []
  (let [repos (cache/get-repos)]
    (doall
     (map (fn [repo] (delete-repo (repo "name"))) repos))
    ))


(defn delete-team [team teamid]
  (do
    (println (str "Deleting Team: " team))
    (println (constants/api-delete-team teamid))

    (client/delete (constants/api-delete-team teamid)
                   {:basic-auth [constants/api-name constants/api-key]} 
                   )

    )
  )

(defn get-all-teams []
  (cache/json-load-uri-autonext constants/api-all-teams)
  )

(defn except-owners [teams]
  (filter (fn [team] (not (= "Owners" (team "name")))) teams)
  )

(defn delete-all-teams []
  (let [teams (get-all-teams)]
    (doall
     (map (fn [team] (delete-team (team "name") (team "id"))) (except-owners teams))
     )
    )
  )









