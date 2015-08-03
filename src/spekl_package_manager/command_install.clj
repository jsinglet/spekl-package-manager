(ns spekl-package-manager.command-install
  (:require [clojure.java.io :as io]
            [clojure.tools.logging :as log]
            [spekl-package-manager.util :as util]
            [spekl-package-manager.constants :as constants]
            [spekl-package-manager.download :as download]
            [spekl-package-manager.package :as package]
            )
  (:import (org.yaml.snakeyaml.scanner ScannerException)
           (org.spekl.spm.utils PackageLoadException
                                CantFindPackageException)))
 

(defn print-missing-deps [deps]
  (log/info "[command-install] Will install the following missing packages:")
  (doseq [x deps] (log/info "[command-install] - " (package/package-name-version x))))

(defn run-install-script [package-description asset-env]
  (let [install-script (package/gather-install-commands package-description)]
    (doall     
     (map (fn [x]
            (log/info "[command-install-scripts] " (x :description) "[" (x :cmd) " =>" (util/command-to-spm-cmd asset-env (x :cmd)) "]")
            ;; note here that "dir" is optional and defaults to the .spm directory
            (util/execute-command-in-directory asset-env (x :cmd) (str (package/make-package-path package-description) (x :dir)))
            ) install-script))))


(defn cleanup-files [assetenv]
  (doall (map (fn [x]
                (log/info "[command-install] Cleaning up resources for asset " (x :name) "[will delete: " (x :real-path) "]")
                (io/delete-file (x :real-path))
                ) assetenv))
  )
(defn indent-line [level]
  (if (= nil level)
    ""
    (apply str (repeat (* 5 level) "-"))
    )
  )

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
    (package/create-needed-dirs package-description)
    
    (log/info "[command-install] Downloading Required Assets... ")

    ;; Step 2: Download all the required assets
    (let [assets (package/extract-assets package-description)]
      (let [assetenv (doall (map (fn [x]
                                   {
                                    :local-file (package/effective-asset-path package-description x)
                                    :effective-path  (.substring (download/download-to (x :name) (x :url) (package/make-asset-path package-description x)) 5) ;; we trim off the .spm\ bit (hence the substring)
                                    :symbol-name (x :asset)
                                    :name        (x :name)
                                    :real-path   (package/make-asset-path package-description x)
                                    })
                           assets))]
        (do
          ;; Step 3: Run the installation commands
          ;;
          ;; Baked into the assumptions for these commands is the following:
          ;;
          ;;
          ;; 1) All assets will be located in a directory .spm/<package-name>-<version>/ as <package-name>-<asset-name> in that directory
          ;; 2) The current working directory of every command will be .spm/<package-name>-<version>/
          ;; 3) After installation, all files downloaded will be deleted.
          (log/info "[command-install] Running package-specific installation commands")
          (run-install-script package-description assetenv)

          ;; Step 4: Cleanup any downloaded files.
          (log/info "[command-install] Performing cleanup tasks...")
          (cleanup-files assetenv)
          ;; Step 5: Write out package description
          (log/info "[command-install] Writing out package description...")
          (package/write-description package-description)
          
          )
        (log/info "[command-install] Completed installation of package" (package/package-name-version package-description))
      ))


    )
  
  )

;;(install-package (package/accuire-remote-package "openjml" '() ))

(defn what-are-we-working-with? []
  (if (.exists (io/as-file (str (constants/package-filename))))
    :package
    (if (.exists (io/as-file (str (constants/project-filename))))
      :project
    )))



(defn tools-for-project [project]
  (map (fn [x]
         (list ((x :tool) :name) ((x :tool) :version))
         ) (project :checks))

  )

(defn specs-for-project [project]
  ;; extract all specs nodes
  (let [specs (flatten (map (fn [x] (x :specs) )   (project :checks)))]
    (map (fn [x] (list (x :name) (x :version))) specs)
    )
  )

;(tools-for-project (package/read-local-conf (constants/project-filename)))
;(specs-for-project (package/read-local-conf (constants/project-filename)))

;; installs all the things needed for the local project by finding the packages and versions needed to satisfy
;; a project and installing them in sequence.
;; some logic should go here to perhaps reorder the tree in the case circular dependencies are encountered. 
(defn install-project []
  ;; load project configuration 
  (let [spekl-file  (package/read-local-conf (constants/project-filename))]
    (log/info "[command-install] Installing packages for this project...")

    (do
      ;; tools
      (log/info "[command-install] Installing tools....")
      
      (doall
       (map (fn [dep]
              (install-package (package/accuire-remote-package (first dep) (rest dep))))
            (tools-for-project spekl-file)))
      
      ;; specs
      (log/info "[command-install] Installing specs....")
      (doall
       (map (fn [dep]
              (install-package (package/accuire-remote-package (first dep) (rest dep))))
            (specs-for-project spekl-file)))
      )
      (log/info "[command-install] Done. Use `spm check` to check your project.")
  ))

;;
;; 
;; This command works in three different ways.
;; - If an argument is supplied (one argument) we download and install the latest version of the package specified on the command line.
;; - If two arguments are supplied (two arguments) we download and install the version of the package specified by the second argument.
;; - Next we check if the current directory has package.yml file. This indicates we should do a test install of the current package, including
;;   any of the dependencies of that package.
;; - Lastly, we assume that it is a normal project, and in that case we just do an install of the tools, specs, etc that are required for this package
;;   to functionq
;; Note that we DO NOT CARE if people try to install multiple versions of some package. We follow the convention that we just use the most recent version of
;; any given package/tool/spec that is installed.
;;
;;
(defn run [arguments options]
  (do (try
     (let [what (first arguments)]
       (case what
         nil
         ;; since we didn't specify what to do, we need to determine it.
         (case (what-are-we-working-with?)
           :package (install-package (package/accuire-local-package)) ;; install using the package.yml in the current working directory
           :project (install-project);; install using spekl.yml
           (log/info "The current directory is not a package or project directory. Run ``spm init`` before running the install command")
           )
         ;; we specified at least some kind of argument.                         
         (install-package (package/accuire-remote-package what (rest arguments))) ;; download the package description and install it.
         ))
     (catch CantFindPackageException e (log/info "[command-install] Cannot find a package matching that description: " (.getMessage e)))
     (catch ScannerException e (log/info "[command-install] Invalid package description encountered for one or more packages: " (.getMessage e)))
     (catch PackageLoadException e (log/info "[command-install] Unable to load package. " (.getMessage e)))

     ))
  (System/exit 0)
  )


;;
;; TODO:
;; - test version of "install" command that installs the package in the current local directory.
;;
