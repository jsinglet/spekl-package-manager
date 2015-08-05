(ns spekl-package-manager.package
  (:require [clj-yaml.core :as yaml]
            [clojure.java.io :as io]
            [clojure.tools.logging :as log]
            [spekl-package-manager.util :as util]
            [spekl-package-manager.constants :as constants]
            [clojure.core.reducers :as r]
            [clojure.string :as string]

            )
  (:import
    (java.io FileNotFoundException)
    (org.spekl.spm.utils PackageLoadException
                         ProjectConfigurationException
                         CantFindPackageException
                         )))

(declare package-name)
;;
;; Functions for reading configurations
;;
(defn read-local-conf
  [file-name]
  (yaml/parse-string (slurp file-name)))

(defn read-conf [conf]
  (yaml/parse-string conf))


(defn create-package-url [name version]
  (str constants/api-raw constants/org-name "/" name "/" version "/" (constants/package-filename)  ))

(defn create-package-base-url [package-description]
  (str constants/api-raw constants/org-name "/" (package-name package-description) ".git" ))


;;
;; used mostly for debugging, this function allows a LOCAL version of a package file to override a remote one
;;
(defn locate-shadow-package-file [package version file-name]
  (try
    (if (.exists (io/as-file file-name))
      (io/as-file file-name)
      (create-package-url package version)
      )
    (catch NullPointerException e (throw (PackageLoadException. (str  "Cannot load local (resource-based) package file: " file-name))))
    )
  )

(defn read-remote-conf
    ([package version] (read-local-conf (locate-shadow-package-file package version (str "packages/" package "-" version ".yml"))))
    ([package] (read-local-conf (locate-shadow-package-file package "master" (str "packages/" package ".yml")))))

(defn version-to-version-string [version]
 (case version
   '()   "LATEST"
   nil   "LATEST"

     (if (instance? String version)
       version
       (first version)
       )))

(defn accuire-remote-package [package version]
  (log/info "[command-install] Finding package" package "in remote repository")
  ;; TODO replace with remote file reading.
  (try 
   (case version
     '()   (read-remote-conf package)
     nil   (read-remote-conf package)

     (if (instance? String version)
       (read-remote-conf package version)
       (read-remote-conf package (first version))
       )

     )
   (catch FileNotFoundException e (throw (CantFindPackageException. (str "Unable to find package: " package " version: " (version-to-version-string version)))   ))
   ))

;; install the current directory's package or proces the spekl.yml  file
(defn accuire-local-package []
  (if (.exists (io/as-file (constants/project-filename)))
    (do
      (log/info "[command-install-project] Installing tools and specs for this project...")
      (read-local-conf (constants/project-filename)))
    (if (.exists (io/as-file (constants/package-filename)))
      (do
        (log/info "[command-install-package] Picking up package from local directory...")
        (read-local-conf (constants/package-filename)))
      (do 
        (log/info "[command-install] Unable to determine project type! Please run \"spm init\" in this directory.")
        (System/exit 1)
        ))))

(defn is-one-of? [o]
  (not (= nil (o :one-of))))

(defn my-platform? [o]
  (or (= util/get-my-platform (o :platform)) (= (o :platform "all"))))

(defn is-dep? [o] (and (not (is-one-of? o)) (my-platform? o)))


(defn is-package-directory?
  ([] (.exists (io/as-file (constants/package-filename))))
  ([path] (.exists (io/file path (constants/package-filename))))
  )

(defn pick-one-of [what]
  (do
    (println "This package allows you to select from the following optional dependences. Please select one of the following:")
    (doseq [x (map vector (range 0 (count what)) what)]

      (let [idx (x 0) data (x 1)]
        (println (format "%d) %s - %s" idx (data :package) (data :version)))))
    (print "[default: 0] ")
    (flush)
    (let [v (read-line)]
      (try
        (let [idx (read-string v)]
          (if (.contains (range 0 (count what)) idx)
            (nth what idx) ;; recursion!
            (pick-one-of what)
            ))
        (catch Exception e (pick-one-of what))))))


(defn gather-one-ofs [one-ofs]
  (map (fn [x]
         (pick-one-of
          (filter (fn [xx] (my-platform? xx)) (x :one-of)))) one-ofs))

(defn installed? [package version]
  (.exists (io/file (constants/package-directory) (str package "-" version) (constants/package-filename) )))


(defn gather-deps [package-description ]
  (let [package-depends  (package-description :depends)]
    (let [flat-deps  (filter (fn [x] (is-dep? x)) package-depends) configured-deps (gather-one-ofs (filter (fn [x] (is-one-of? x)) package-depends))]
      { 
       :flat-deps flat-deps
       :configured-deps configured-deps
       :all-deps (concat flat-deps configured-deps)
       }
      )))

(defn gather-install-commands [package-description]
  (let [package-install (package-description :install)]
    ;; filter out all the applicable commands
    (filter (fn [cmd-set] (my-platform? cmd-set)) package-install)))

(defn gather-missing-deps [deps]
  (filter (fn [x] (not (installed? (x :package) (x :version)))) deps))

(defn extract-assets [package-description]
  (let [assets (package-description :assets)]
    (filter (fn [x] (my-platform? x)) assets)))

(defn package-name [package-description]
  (if (= nil (package-description :name))
    (package-description :package)
    (package-description :name)))

(defn package-version [package-description]
  (package-description :version))


(defn package-name-version [package-description]
  (str (package-name package-description) " (version: " (package-description :version) ")"))

(defn make-package-path [package-description]
  (.getPath (io/file (constants/package-directory) (str (package-name package-description) "-" (package-description :version))))
  )



(defn ext-or-nil [ext]
  (if (= ext nil)
    nil
    (str "." ext)))

(defn effective-asset-path [package-description asset]
  (let [package (package-name package-description) asset-name (asset :asset) asset-ext (ext-or-nil (asset :kind))]
    (str package "-" asset-name asset-ext)
    ))

(defn make-asset-path [package-description asset]
  (let [package (package-name package-description) asset-name (asset :asset) asset-ext (ext-or-nil (asset :kind))]
  (.getPath (io/file  (make-package-path package-description) (str package "-" asset-name asset-ext)))
  ))


(defn create-needed-dirs [package-description]
  (do
    (util/create-dir-if-not-exists (constants/package-directory))
    (util/create-dir-if-not-exists (make-package-path package-description))
    )
  )


(defn make-package-file-path [package-description]
  (.getPath (io/file (constants/package-directory) (str (package-name package-description) "-" (package-description :version)) (constants/package-filename) ))
  )


(defn write-description [package-description]
  (let [destination (make-package-file-path package-description)]
    (spit destination (yaml/generate-string package-description))
    )
  )


(defn package-description-from-path
  ([] (read-local-conf (.getPath (io/file (constants/package-filename)))))
  ([path] (read-local-conf (.getPath (io/file path (constants/package-filename)))))
  )



(defn load-package-or-false [path]
  (try
    ((read-local-conf (.getPath (io/file path (constants/package-filename)))) :version)
    (catch Exception e (log/info "[package] Skipping invalid package description in directory:" (.getPath path))))
  )

(defn get-all-package-descriptions []
  (let [possible-package-dirs (.listFiles (io/file (constants/package-directory)))]
    (map (fn [package-dir]
           (let [expanded-path (io/file package-dir (constants/package-filename))]
             {
              :file expanded-path
              :dir package-dir
              :description (read-local-conf (.getPath expanded-path))
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
    (let [blank-index (r/reduce (fn [acc x] (assoc acc (package-name x) [])) {} descriptions)]

      (r/reduce (fn [acc x]

                  (let [current-list (acc (package-name x))]
                    (assoc acc (package-name x) (reverse (sort (conj current-list (x :version)))))
                    )


                  ) blank-index descriptions)
      
      
      )
    ))

(defn is-most-current-package [package-description index]
  (.equals (package-description :version) (first (index (package-name package-description)))))

(defn only-current-packages []
  (let [packages (get-all-package-descriptions)]
    (let [indexed-packages (index-packages packages)]
      (filter (fn [package] (is-most-current-package (package :description) indexed-packages)) packages))))




(defn load-configured-checks []
  ((read-local-conf (constants/project-filename)) :checks)
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
  ([name] (let [check  (first (filter (fn [package] (.equals name (package-name (package :description)))) (only-current-packages)))]
            (if (= nil check)
              (throw (PackageLoadException. (str "Packed named \"" name "\" is not installed.")))
              check
              )
            ))
 
  ([name version] (let [check  (first (filter (fn [package] (and  (.equals version ((package :description) :version)) (.equals name (package-name (package :description))))) (get-all-package-descriptions)))]
                    (if (= nil check)
                      (throw (PackageLoadException. (str "Package named \"" name "\" (version: " version  ") is not installed.")))
                      check
                      )
                    ))
  )





(defn resolve-dep [name version]
  (let [packages (get-all-package-descriptions)]
    ;; load the name and version
    (let [resolved  (filter (fn [x] (and
                                     (.equals ((x :description) :name)    name)
                                     (.equals ((x :description) :version) version)
                                     )) packages)]

      ;; make sure something was resovled
      (if (= (count resolved) 0)
        (throw (PackageLoadException. (str "Some required dependencies are missing. Missing Package: " name " version: " version)))
        ;; ok, give it back
        (let [resolved-package (first resolved)]
         {
          :dir (resolved-package :dir)
          :description (resolved-package :description)
          :name ((resolved-package :description) :name)
          :version ((resolved-package :description) :version)

          })
        ))))
;;
;; takes a package description and resolves required dependencies to
;; exact packages that are installed.
;; it throws an error if some can't be found
;;
(defn missing-dep-list [deps]
  (do
    (doseq [x deps] (log/info "[package-info] - " (package-name-version x)))
    ""
    )
 )

(defn resolve-deps [package-description]
  ;; step one: find the maximal set of dependencies
   (let [deps (gather-deps package-description)]
     (let [missing-deps (gather-missing-deps (deps :all-deps))]

       ;; is everything installed?
       (if (> (count missing-deps) 0)
         (throw (PackageLoadException. (str "Some required dependencies are missing. See the output above for more information" (missing-dep-list missing-deps))))
         ;; yes, let's start building the structure
         (map (fn [x] (resolve-dep (x :package) (x :version))) (deps :all-deps))
         ))))



(defn index-resolved-deps [resolved-deps]
  (r/reduce (fn [acc x] (assoc acc (x :name) x)) {} resolved-deps))





