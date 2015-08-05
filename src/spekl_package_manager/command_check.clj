(ns spekl-package-manager.command-check
  (:require [clojure.java.io :as io]
            [clojure.tools.logging :as log]
            [spekl-package-manager.util :as util]
            [spekl-package-manager.constants :as constants]
            [spekl-package-manager.download :as download]
            [spekl-package-manager.package :as package]
            [spekl-package-manager.check :as check]
            [clojure.core.reducers :as r]
            [org.satta.glob :as glob]
            [clojure.string :as string]
            [clojure.java.shell :as shell]
            
            )

    (:import
    (java.io FileNotFoundException)
    (org.spekl.spm.utils PackageLoadException
                         ProjectConfigurationException))
  )



;; expands a string like [src/*.java]
(defn expand-glob [globs]
  (let [groups (map (fn [x] (glob/glob (.trim x))) globs)]
    ;; combine
    (map (fn [x] (.getPath x)) (r/reduce (fn [acc x] (concat acc x)) []  groups))
    ))


(defn path-resolve [env]
  (let [package-base (env :path-to-package) package-index (env :resolved-packages)]
    (fn
      ;; write this
      ([package asset] (.toString (.resolve (.toPath (io/file ((package-index package) :dir))) asset)))
      ([asset] (.toString (.resolve (.toPath (io/file package-base)) asset)))  
      ))
  )


;; (defmacro loadcheck
;;   [name & body]
;;   `(fn [] (~(symbol ("spekl-package-manager.check/" name ))))
;;   )





;(macroexpand '(loadcheck a))

;(loadcheck a)
;


;; read the package file for the package that runs the tool
;; build a map for each package it DEPENDS on
;; :file, :dir, :description

(defn run-configured-check [configuration]
  (let [package (configuration :package-data) config (configuration :configured-check)]
   (do
     (log/info "[command-check] Running check:" (config :description))
     (load-file (.getPath (io/file (package :dir) (constants/check-file))))
     ;;
     ;; create the arguments for this check
     ;;

     ;;
     ;; :path-to-package : the full path to the package we are going to execute the command from
     ;; :project-files   : the expanded list of files specified by the check
     ;; :specs           : extract the specs 
     ;; 
     
     ;; (path-resolve "openjml" "openjml.jar")
     ;; (path-resolve "openjml.jar")

     (binding [check/*resolver* (path-resolve
                           {
                            :path-to-package (package :dir)
                            :resolved-packages (package/index-resolved-deps (package/resolve-deps (package :description)))
                            })

               check/*project-files-string*  (string/join " " (expand-glob (config :paths)))
               check/*project-files*  (string/join " " (expand-glob (config :paths)))
               check/*specs* (package/get-required-specifications (config :specs))
               ] 
       

       ((ns-resolve 'spekl-package-manager.check (symbol (config :check))))
       )
     ))
  )

;((resolve (symbol "check" "default")))
        ;;
        ;; pass in the rest of the raw environment 
        ;;
        ;; {
        ;;  :specs (package/get-required-specifications (config :specs))
        ;;  :project-files (expand-glob (config :paths))
        ;;  :project-files-string (string/join " " (expand-glob (config :paths)))
        ;;  }



(defn create-run-configuration [configured-check package-data]
  {
   :configured-check configured-check
   :package-data     package-data
   }
  )


(defn infer-check-tool-version [check-name]
  (let [check (package/locate-configured-check check-name)]
    (if (= nil ((check :tool) :version))
      (((package/locate-package-check ((check :tool) :name)) :description) :version)
      (((check :check) :tool) :version))))



(defn infer-check-target [check-name]
  (let [check (package/locate-configured-check check-name)]
    (if (= nil (check :check))
      "default"
      (check :check))))

(defn locate-and-run-check [check-name]
  (let [tool-name (((package/locate-configured-check check-name) :tool) :name) check-target (infer-check-target check-name) tool-version (infer-check-tool-version check-name)]
    (run-configured-check (create-run-configuration
                           (package/locate-configured-check check-name)
                           (package/locate-package-check    tool-name tool-version)
                           )
                          ))
  )


(defn run-all-checks []
  (let [checks (package/load-configured-checks)]
    (log/info "[command-check] Running all checks for project...")
    (doall (map (fn [check]
                  (locate-and-run-check (check :name))
            ) checks))
    ))


(defn run-check [name rest]
  (locate-and-run-check name))

;;
;; This command can take an optional argument that specifies which "check" to run. If no argument is specified, it should run all checks in sequence. 
;;
(defn run [arguments options]
  (do (try
     (let [what (first arguments)]
       (case what
         ;; run all checks
         nil (run-all-checks)
         ;; run a specific check
         (run-check what (rest arguments))
         ))
     (catch PackageLoadException e (log/info "[command-check] One or more packages are missing. Please run `spm install` to install them." (.getMessage e)))
     (catch ProjectConfigurationException e (log/info "[command-check] There is an error in your project configuration:" (.getMessage e)))
     (catch Exception e (log/info "[command-check] Encountered a problem while checking: " (.printStackTrace e)))
     ))
    (System/exit 0)
  )

;(run '() nil)


;;(load-file ".spm/openjml-esc-WORK/check.clj")
;;''(check/check)



