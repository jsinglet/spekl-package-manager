(ns spekl-package-manager.command-install
  (:require [clojure.java.io :as io]
            [clojure.tools.logging :as log]
            [spekl-package-manager.util :as util]
            [spekl-package-manager.constants :as constants]
            [spekl-package-manager.download :as download]
            [spekl-package-manager.package :as package]
            )
  (:import (org.yaml.snakeyaml.scanner ScannerException)))
 

(defn print-missing-deps [deps]
  (log/info "[command-install] Will install the following missing packages:")
  (doseq [x deps] (log/info "[command-install] - " (package/package-name-version x))))

(defn run-install-script [package-description asset-env]
  (let [install-script (package/gather-install-commands)]
    
    ))

(defn install-package [package-description]
  (log/info "[command-install] Starting install of package" (package/package-name-version package-description))

  (do
    ;; Step 0: install anything it depends on (these will always fetch from remote)
    (log/info "[command-install] Examining dependencies...")
    (let [deps (package/gather-deps package-description)]
      (let [missing-deps (package/gather-missing-deps (deps :all-deps))]
        (if (>  (count missing-deps) 0)
          (do
            (print-missing-deps missing-deps)
            (doseq [pkg missing-deps] (install-package (package/accuire-remote-package (pkg :package) (pkg :version))))
            ))
        ))
    
    (log/info "[command-install] Installing package" (package/package-name-version package-description))

    ;; Step 1: Directories. Create the .spm and package directory where we will do our work
    (util/create-dir-if-not-exists (constants/package-directory))

    (util/create-dir-if-not-exists (constants/package-directory))


    (log/info "[command-install] Downloading Required Assets... ")

    ;; Step 2: Download all the required assets
    (let [assets (package/extract-assets package-description)]
      (let [assetenv (map (fn [x]
             {
              :local-file  (download/download-to (x :name) (x :url) (package/make-asset-path (package/package-name package-description) (x :asset)))
              :symbol-name (x :asset)
              })
                          assets)]
        ;; Step 3: Run the installation commands
        ;;
        ;; Baked into the assumptions for these commands is the following:
        ;;
        ;;
        ;; 1) All assets will be located in a directory .spm/<package-name>-<asset-name>/
        ;; 2) The current working directory of every command will be .spm/<package-name>/
        ;; 3) After installation, all files downloaded will be deleted.
        ;; 4)
        
        
        
      ))


    )
  
  )

(defn what-are-we-working-with? []
  (if (.exists (io/as-file (str (constants/package-filename))))
    :package
    (if (.exists (io/as-file (str (constants/project-filename))))
      :project
    )))

;; installs all the things needed for the local project. 
(defn install-project []
  (log/info "Feature not implemented yet."))

;;
;; 
;; This command works in three different ways.
;; - If an argument is supplied (one argument) we download and install the latest version of the package specified on the command line.
;; - If two arguments are supplied (two arguments) we download and install the version of the package specified by the second argument.
;; - Next we check if the current directory has package.yml file. This indicates we should do a test install of the current package, including
;;   any of the dependencies of that package.
;; - Lastly, we assume that it is a normal project, and in that case we just do an install of the tools, specs, etc that are required for this package
;;   to function
;; Note that we DO NOT CARE if people try to install multiple versions of some package. We follow the convention that we just use the most recent version of
;; any given package/tool/spec that is installed.
;;
;;
(defn run [arguments options]
  (try
    (let [what (first arguments)]
      (case what
        nil
        ;; since we didn't specify what to do, we need to determine it.
        (case (what-are-we-working-with?)
          :package (install-package (package/accuire-local-package))                 ;; install using the package.yml in the current working directory
          :project (log/info "not implemented yet")                          ;; install using spekl.yml
          (log/info "The current directory is not a package or project directory. Run ``spm init`` before running the install command")
          )
        ;; we specified at least some kind of argument.                         
        (install-package (package/accuire-remote-package what (rest arguments)))     ;; download the package description and install it.
        ))
  (catch ScannerException e (log/info "[command-install] Invalid package description encountered for one or more packages: " (.getMessage e)))))


;;(run '("openj") nil)
;;(extract-assets (accuire-remote-package "openjml" '()))  ;;(run '("openjml") nil)

;;(gather-missing-deps (gather-deps (accuire-remote-package "openjml" '()))

;(first '("openjml"))

;;(> (count (gather-missing-deps ((gather-deps (accuire-remote-package "openjml" nil)) :all-deps))) 1)



                                        ;(let [package-description (accuire-remote-package "openjml" '())]
                                        ;  (let [downloads (map (fn [x] {
                                        ;                                :local-file (download/download-to (x :name) (x :url) (make-asset-path (package-name package-description) (x :asset)))
                                        ;                                :symbol-name (x :asset)
                                        ;                                } ) (extract-assets package-description))]
                                        ;     downloads
                                        ;   ))



                                        ;(= '() null)


