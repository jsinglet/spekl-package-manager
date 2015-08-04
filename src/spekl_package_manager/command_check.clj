(ns spekl-package-manager.command-check
  (:require [clojure.java.io :as io]
            [clojure.tools.logging :as log]
            [spekl-package-manager.util :as util]
            [spekl-package-manager.constants :as constants]
            [spekl-package-manager.download :as download]
            [spekl-package-manager.package :as package]
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



(defn load-package-or-false [path]
  (try
    ((package/read-local-conf (.getPath (io/file path (constants/package-filename)))) :version)
    (catch Exception e (log/info "[command-check] Skipping invalid package description in directory:" (.getPath path))))
  )

(defn get-all-package-descriptions []
  (let [possible-package-dirs (.listFiles (io/file (constants/package-directory)))]
    (map (fn [package-dir]
           (let [expanded-path (io/file package-dir (constants/package-filename))]
             {
              :file expanded-path
              :dir package-dir
              :description (package/read-local-conf (.getPath expanded-path))
              }
             )
           ) (filter (fn [dir] (load-package-or-false dir)) possible-package-dirs))
    )
  )

(defn package-is-required-spec [package specs]
  (> (count (filter (fn [x] (and (.equals (x :name) (package :name))   (.equals (x :version) (package :version))  )) specs)) 0)
  )

;;
;; TODO - throw an error if any specs can't be found
;;
(defn get-required-specifications [specs]
  (let [all-packages (get-all-package-descriptions)]
    (let [found-specs  (filter (fn [x] (package-is-required-spec (x :description) specs)) all-packages)]

      ;; make sure we found everything we tried to find
      (if (= (count found-specs) (count specs))
        found-specs
        (throw (PackageLoadException. (str "Some specification packages were not found.")))
        )

      )))

;; to do this the following is done
;; we create a hash grouping :package_name => [installed packages]
;; we sort [installed packages]
;; we pass over each of the :package names and map it to the first element of installed packages.
(defn index-packages [packages]
  (let [descriptions (map (fn [package] (package :description)) packages)]
    (let [blank-index (r/reduce (fn [acc x] (assoc acc (package/package-name x) [])) {} descriptions)]

      (r/reduce (fn [acc x]

                  (let [current-list (acc (package/package-name x))]
                    (assoc acc (package/package-name x) (reverse (sort (conj current-list (x :version)))))
                    )


                  ) blank-index descriptions)
      
      
      )
    ))

(defn is-most-current-package [package-description index]
  (.equals (package-description :version) (first (index (package/package-name package-description)))))

(defn only-current-packages []
  (let [packages (get-all-package-descriptions)]
    (let [indexed-packages (index-packages packages)]
      (filter (fn [package] (is-most-current-package (package :description) indexed-packages)) packages))))

(declare check)

;; expands a string like [src/*.java]
(defn expand-glob [globs]
  (let [groups (map (fn [x] (glob/glob (.trim x))) globs)]
    ;; combine
    (map (fn [x] (.getPath x)) (r/reduce (fn [acc x] (concat acc x)) []  groups))
    ))

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
     (check {
             :specs (get-required-specifications (config :specs))
             :path-to-package (package :dir)
             :project-files (expand-glob (config :paths))
             :project-files-string (string/join " " (expand-glob (config :paths)))
       })
     ))
  )


(defn load-configured-checks []
  ((package/read-local-conf (constants/project-filename)) :checks)
  )

(defn locate-configured-check
  ([name] (let [check  (first (filter (fn [x] (.equals name (x :name))) (load-configured-checks)))]
            (if (= nil check)
              (throw (ProjectConfigurationException. (str "Check named \"" name "\" is not configured in spekl.yml")))
              check
              )
            ))
  ([name version] (let [check  (first (filter (fn [x] (and ((x :tool) :version) (.equals name (x :name)))) (load-configured-checks)))]
                    (if (= nil check)
                      (throw (ProjectConfigurationException. (str "Check named \"" name "\" is not configured in spekl.yml")))
                      check
                      )
                    ))
  )

(defn locate-package-check
  ([name] (let [check  (first (filter (fn [package] (.equals name (package/package-name (package :description)))) (only-current-packages)))]
            (if (= nil check)
              (throw (PackageLoadException. (str "Packed named \"" name "\" is not installed.")))
              check
              )
            ))
 
  ([name version] (let [check  (first (filter (fn [package] (and  (.equals version ((package :description) :version)) (.equals name (package/package-name (package :description))))) (get-all-package-descriptions)))]
                    (if (= nil check)
                      (throw (PackageLoadException. (str "Package named \"" name "\" (version: " version  ") is not installed.")))
                      check
                      )
                    ))
  )

(defn create-run-configuration [configured-check package-data]
  {
   :configured-check configured-check
   :package-data     package-data
   }
  )

(defn locate-and-run-check
  ([name] (let [version (((locate-configured-check name) :tool) :version)] (run-configured-check (create-run-configuration

                                                                                         (locate-configured-check name) (locate-package-check name version))


                                        )))
  ([name version] (run-configured-check (create-run-configuration (locate-configured-check name version) (locate-package-check name version))))
  )


(defn run-all-checks []
  (let [checks (load-configured-checks)]
    (log/info "[command-check] Running all checks for project...")
    (doall (map (fn [check]
            (locate-and-run-check ((check :tool) :name) ((check :tool) :version))
            ) checks))
    ))


(defn run-check [name rest]
  (let [version (first rest)]
    (case version
      nil (locate-and-run-check name)
      (locate-and-run-check name version))))

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
;;  (System/exit 0)
  )

;;(run '() nil)







