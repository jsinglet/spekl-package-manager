(ns spekl-package-manager.command-init
  (:require [clojure.tools.cli :refer [parse-opts]]
            [clojure.string :as string]
            [clj-yaml.core :as yaml]
            [clojure.java.io :as io]
            [spekl-package-manager.templates :as templates]
            [spekl-package-manager.prompts :as prompt]
            [clojure.tools.logging :as log]
            [spekl-package-manager.constants :as const]
            [spekl-package-manager.package   :as package]
            [spekl-package-manager.backend   :as backend]
  ))

;;
;; Prompt configuration
;;
(def prompts {
              :configure-project [(prompt/create "Project Name?" "my project")
                                  (prompt/create "Project Id?"  "my.project")
                                  (prompt/create "Project Version?" "0.0.1")]

              :configure-tool    [(prompt/create "Tool Name?" "my tool")
                                  (prompt/create "Tool Description?" "a short description")
                                  (prompt/create "Version?"   "0.0.1")
                                  (prompt/create "Author Name?"   "Some User")
                                  (prompt/create "Author Email?"  "user@email.com")
                                  (prompt/create "Username? (not stored)"      "someuser")

                                  ]

              :configure-spec    [(prompt/create "Spec Name?" "my spec")
                                  (prompt/create "Spec Description?" "a short description")
                                  (prompt/create "Version?"   "0.0.1")
                                  (prompt/create "Author Name?"   "Some User")
                                  (prompt/create "Author Email?"   "user@email.com")
                                  (prompt/create "Username? (not stored)"      "someuser")
                                  ]

              })

;;
;; 
;;


(defn init-project-with-config [project-file]
  (log/info "[new-project]" "Writing project file to spekl.yml")
  (spit (const/project-filename) project-file)
  (log/info "[new-project]" "Done."))


(defn init-tool-with-config [project-file username]
  (do
    (backend/register-project ((package/read-conf project-file) :name) username)
    (if (.exists (io/as-file (const/project-filename)))
      (do ;; if it's part of a project, we do our work in .spm
        (log/info "[new-tool]" "Writing configuration file to: " (package/make-package-file-path (package/read-conf project-file)))
        (package/create-needed-dirs (package/read-conf project-file))
        (spit (package/make-package-file-path (package/read-conf project-file)) project-file)
        (backend/init-at (package/make-package-path (package/read-conf project-file)))
        )
      (do ;; otherwise we do our work in the .spm directory
        (log/info "[new-tool]" "Writing configuration file to package.yml")
        (spit (const/package-filename) project-file)
        (backend/init)
        )
      )
    (log/info "[new-tool]" "Done."))

    )
  

(defn init-spec-with-config [project-file username]
  (do
    (backend/register-project ((package/read-conf project-file) :name) username)
    (if (.exists (io/as-file (const/project-filename)))
      (do ;; if it's part of a project, we do our work in .spm
        (log/info "[new-spec]" "Writing configuration file to:" (package/make-package-file-path (package/read-conf project-file)))
        (package/create-needed-dirs (package/read-conf project-file))
        (spit (package/make-package-file-path (package/read-conf project-file)) project-file)
        (backend/init-at (package/make-package-path (package/read-conf project-file)))
        )
      (do ;; otherwise we do our work in the .spm directory
        (log/info "[new-spec]" "Writing configuration file to package.yml")
        (spit (const/package-filename) project-file)
        (backend/init)
        )
      )
    (log/info "[new-spec]" "Done.")))

(defn run-spec [arguments]
  (log/info "[new-spec]" "Creating new spec project...")

  (let [[project-name project-description project-version author-name author-email author-username] (prompt/fill prompts :configure-spec)]
    (let
      [project-file (templates/template-with-params
                      "minimal-spec"
                      {
                       "project_name" project-name
                       "project_description" project-description
                       "project_version" project-version
                       "author_name" author-name
                       "author_email" author-email
                       })
       ]
      (prompt/looks-reasonable
        project-file
        (fn [] (init-spec-with-config project-file author-username))
        (fn [] (run-spec arguments)))
      )))

(defn run-tool [arguments]
  (log/info "[new-tool]" "Creating new tool project...")

  (let [[project-name project-description project-version author-name author-email author-username] (prompt/fill prompts :configure-tool)]
    (let
      [project-file (templates/template-with-params
                      "minimal-tool"
                      {
                       "project_name" project-name
                       "project_description" project-description
                       "project_version" project-version
                       "author_name" author-name
                       "author_email" author-email
                       })
       ]
      (prompt/looks-reasonable
        project-file
        (fn [] (init-tool-with-config project-file author-username))
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
    "project"  (run-project (rest arguments))
    (run-project (rest arguments))))

