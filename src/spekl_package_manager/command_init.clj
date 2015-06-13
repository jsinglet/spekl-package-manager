(ns spekl-package-manager.command-init
  (:require [clojure.tools.cli :refer [parse-opts]]
            [clojure.string :as string]
            [clj-yaml.core :as yaml]
            [clojure.java.io :as io]
            [spekl-package-manager.templates :as templates]
            [spekl-package-manager.prompts :as prompt]
            [clojure.tools.logging :as log]
            [spekl-package-manager.constants :as const]
  ))

;;
;; Prompt configuration
;;
(def prompts {
              :configure-project [(prompt/create "Project Name?" "my project")
                                  (prompt/create "Project Id?"  "my.project")
                                  (prompt/create "Project Version?" "0.0.1")]

              :configure-tool    [(prompt/create "Tool Name?" "my tool")
                                  (prompt/create "Version?"   "0.0.1")]

              :configure-spec    [(prompt/create "Spec Name?" "my spec")
                                  (prompt/create "Version?"   "0.0.1")]

              })

;;
;; 
;;


(defn init-project-with-config [project-file]
  (log/info "[new-project]" "Writing project file to spekl.yml")
  (spit (const/project-filename) project-file)
  (log/info "[new-project]" "Done."))

(defn init-tool-with-config [project-file]
  (log/info "[new-tool]" "Writing configuration file to package.yml" const/package-filename)
  (spit (const/package-filename) project-file)
  (log/info "[new-tool]" "Done."))

(defn init-spec-with-config [project-file]
  (log/info "[new-spec]" "Writing configuration file to package.yml")
  (spit (const/package-filename) project-file)
  (log/info "[new-spec]" "Done."))


(defn run-spec [arguments]
  (log/info "[new-spec]" "Creating new spec project...")

  (let [[project-name project-version] (prompt/fill prompts :configure-spec)]
    (let
      [project-file (templates/template-with-params
                      "minimal-spec"
                      {
                       "project_name" project-name
                       "project_version" project-version
                       })
       ]
      (prompt/looks-reasonable
        project-file
        (fn [] (init-spec-with-config project-file))
        (fn [] (run-spec arguments)))
      )))

(defn run-tool [arguments]
  (log/info "[new-tool]" "Creating new tool project...")

  (let [[project-name project-version] (prompt/fill prompts :configure-tool)]
    (let
      [project-file (templates/template-with-params
                      "minimal-tool"
                      {
                       "project_name" project-name
                       "project_version" project-version
                       })
       ]
      (prompt/looks-reasonable
        project-file
        (fn [] (init-tool-with-config project-file))
        (fn [] (run-tool arguments)))
      )))


(defn run-project [arguments]
  (log/info "[new-project]" "Creating new verification project...")
  (let [[project-name project-id project-version] (prompt/fill prompts :configure-project)]
    (let
        [project-file (templates/template-with-params
                       "minimal-project"
                       {
                        "project_name" project-name
                        "project_id"   project-id
                        "project_version" project-version
                        })
         ]
      (prompt/looks-reasonable
       project-file
       (fn [] (init-project-with-config project-file))
       (fn [] (run-project arguments)))
      )))



(defn run [arguments options]
  (log/info "[command-init]")
  (case (first arguments)
    "specs" (run-spec (rest arguments))
    "spec"  (run-spec (rest arguments))
    "tool"  (run-tool (rest arguments))
    "project"  (run-tool (rest arguments))
    (run-project (rest arguments))))

