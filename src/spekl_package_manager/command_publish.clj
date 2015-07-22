(ns spekl-package-manager.command-publish

  (:require [clojure.java.io :as io]
            [clojure.tools.logging :as log]
            [spekl-package-manager.util :as util]
            [spekl-package-manager.constants :as constants]
            [spekl-package-manager.download :as download]
            [spekl-package-manager.backend :as backend]
            [spekl-package-manager.package :as package]
            )

  )


;;
;; assumes you are in a directory that is a package to publish.
;; 

;;
;; Steps to publish
;;

;; 1) Make sure we have a .git repo
;; 2) Make sure we have a package.yml
;; 3) See if we have any branches. If no, goto: initial commit if yes, goto with branches.



;;
;; STAGE: With Branches
;;

;; If the current branch == the version in the package file (assumes remote was already added):
;; - do a git add . => commit =>  push
;; If the current branch != the version in the package file
;; - git branch -b <version>
;; - git add . => commit => push
;;

;;
;; STAGE: Initial Commit
;;
;; 1) git add .
;; 2) git commit -m ""
;; 3) git branch -b <version>
;; 4) add remote to master repository (repo-name)
;; 5) push

(def current-package ".")


(defn do-sanity-check []
  (do
    ;; 
    ;; CHECK #1: is this a real package directory?
    ;;
    (log/info "[command-publish] Checking... Current directory is a package directory")
    (if (= false package/is-package-directory?)
     (throw (Exception. "Current directory is not a package directory."))
     )

    (let [package-description (package/package-description-from-path)]
      ;;
      ;; CHECK #2: are they trying to publish something already published?
      ;;
      (log/info "[command-publish] Checking... That the current package version is unpublished...")
      (let [version (package-description :version)]

        (if (util/in? (backend/get-versions current-package) version)
          (throw (Exception. (str "The version (" version ") listed in your package.yml file has already been published. Please increment this value and try again.")))
          )


        )
      

      
      
      )
    ))

(defn publish []
  (do
    (log/info "[command-publish] Performing pre-publishing sanity check...")
    (do-sanity-check)
    ;; after the sanity check we assume:
    ;; 1. package.yml exists (and it's valid)
    ;; 2. that the current version has NOT been published yet
    ;; 3. at a minimum a git init had been done.

    (let [package-description (package/package-description-from-path)]
      (let [version (package-description :version)]
        
        ;; step 1 - create the new version
        (log/info "[command-publish] Creating new version:" version)
        (backend/new-version current-package version)

        ;; step 2 - sync all the changes to the server
        (log/info "[command-publish] Publishing changes to the repository")
        (backend/sync-version current-package)
        
        (log/info "[command-publish] Done.")
        )
      )))


(defn run [arguments options]
  (do (try
     (let [what (first arguments)]
       (case what
         nil (publish)  ;; try to figure it out
         (log/info "[command-publish] The PUBLISH command does not take any arguments and should be executed from a package directory.")
         ))
     (catch Exception e (log/info "[command-publish] Error during publishing:" (.getMessage e)))
     ))
  (System/exit 0)
  )


