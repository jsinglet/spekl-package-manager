(ns spekl-package-manager.command-extend
  (:require [clojure.tools.cli :refer [parse-opts]]
            [clojure.string :as string]
            [clj-yaml.core :as yaml]
            [clojure.java.io :as io]
            [spekl-package-manager.templates :as templates]
            [spekl-package-manager.prompts :as prompt]
            [clojure.tools.logging :as log]
            [spekl-package-manager.constants :as const]
            [spekl-package-manager.package   :as package]
            [spekl-package-manager.backend   :as backend]))


(def prompts {

              :configure-spec    [
                                  (prompt/create "Extends?" "existing-package-name")
                                  (prompt/create "Spec Name?" "my spec")
                                  (prompt/create "Spec Description?" "a short description")
                                  (prompt/create "Version?"   "0.0.1")
                                  (prompt/create "Author Name?"   "Some User")
                                  (prompt/create "Author Email?"   "user@email.com")
                                  (prompt/create "Username? (not stored)"      "someuser")
                                  ]

              :configure-spec-no-extend    [(prompt/create "Spec Name?" "my spec")
                                  (prompt/create "Spec Description?" "a short description")
                                  (prompt/create "Version?"   "0.0.1")
                                  (prompt/create "Author Name?"   "Some User")
                                  (prompt/create "Author Email?"   "user@email.com")
                                  (prompt/create "Username? (not stored)"      "someuser")
                                  ]

              })


(defn locate-extended-package [name]
  (package/accuire-remote-package name nil))

(defn extend-spec-with-config [project-file username]
  (let [conf (package/read-conf project-file)]
    (let [extend-package (locate-extended-package (conf :extends))]
     (do
       (backend/register-project (conf :name) username)
       (if (.exists (io/as-file (const/project-filename)))
         (do ;; if it's part of a project, we do our work in .spm
           (log/info "[extend-spec]" "Writing configuration file to:" (package/make-package-file-path (package/read-conf project-file)))
           (package/create-needed-dirs conf)        
           (backend/extend-package conf extend-package)
           (spit (package/make-package-file-path conf) project-file)
           (backend/save-version (package/make-package-path conf)))
         (do ;; otherwise we do our work in the .spm directory
           (log/info "[extend-spec]" "Writing configuration file to package.yml")
           (backend/extend-package conf extend-package ".")
           (spit (const/package-filename) project-file)
           (backend/save-version ".")
           ))
       (log/info "[extend-spec]" "Done.")))))


(declare run-spec-with-config)

(defn run-spec
  ([]  (apply run-spec-with-config (prompt/fill prompts :configure-spec)))
  ([extends-what]  (apply run-spec-with-config (cons extends-what (prompt/fill prompts :configure-spec-no-extend))) ))


(defn run-spec-with-config [extends project-name project-description project-version author-name author-email author-username]
  (log/info "[extend-spec]" "Creating new spec extension project..." extends project-name project-description project-version author-name author-email author-username)

  (let
      [project-file (templates/template-with-params
                     "extend-spec"
                     {
                      "project_name" project-name
                      "extends" extends
                      "project_description" project-description
                      "project_version" project-version
                      "author_name" author-name
                      "author_email" author-email
                      })
       ]
    (prompt/looks-reasonable
     project-file
     (fn [] (extend-spec-with-config project-file author-username))
     (fn [] (run-spec)))))
  

(defn run [arguments options]
  (log/info "[command-extend]")
  (do
    (try
      (case (first arguments)
        ;; we are going to prompt them for the package to extend
        nil (run-spec)
        ;; otherwise they specified it.
        (run-spec (first arguments)))
      (catch Exception e (log/info "[command-extend] Error:" (.getMessage e))))
    (System/exit 0)))

