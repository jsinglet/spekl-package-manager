(ns spekl-package-manager.command-refresh
  (:require [clojure.tools.cli :refer [parse-opts]]
            [clojure.string :as string]
            [clj-yaml.core :as yaml]
            [clojure.java.io :as io]
            [spekl-package-manager.templates :as templates]
            [spekl-package-manager.prompts :as prompt]
            [clojure.tools.logging :as log]
            [spekl-package-manager.constants :as constants]
            [spekl-package-manager.package   :as package]
            [spekl-package-manager.backend   :as backend]))



;;
;; works on a spekl.yml file or during specification development
;;


(defn get-mode []
  (if (.exists (io/as-file (constants/project-filename)))
    :project
    :spec))


;; recurr on package-description
(defn refresh-spec [dest package-description]
  (let [extended (package-description :extends)]
    (if (= nil extended)
      ;; doesn't extend anything
      (log/info "[command-refresh] Package" (str "`" (package-description :name) "`") "does not need to be refreshed.")
      ;; it does!
      (do
        ;; pull in the changes
        (log/info "[command-refresh] Pulling in latest changes to upstream package" (str "`" extended "`"))
        ;; see if the upstream package needs to be refreshed
        (let [remote-package (package/accuire-remote-package extended nil)]
          (backend/refresh-remote remote-package dest)
          (refresh-spec dest remote-package))))))

(defn get-project-specs []
  (let [checks (package/load-configured-checks)]
    (flatten (map (fn [x] (package/get-required-specifications (x :specs))) checks))))

(defn refresh-spekl-project []
    (log/info "[command-refresh] Refreshing specs in this project...")
    (let [checks (package/load-configured-checks)]
      (doall
       (map (fn [x]  (refresh-spec (package/make-package-file-path (x :description)) (x :description)))
            (get-project-specs))
       )))


(defn refresh-spec-project []
  (log/info "[command-refresh] Refreshing specs in this specification project...")
  (refresh-spec "."  (package/read-local-conf (constants/package-filename))))

(defn run [arguments options]
  (log/info "[command-refresh]")
  (do
    (try
      (if (= (get-mode) :project)
        (refresh-spekl-project)
        (refresh-spec-project))
      (catch Exception e (log/info "[command-refresh] Error:" (.getMessage e))))
    (System/exit 0)))






