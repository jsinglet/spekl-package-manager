(ns spekl-package-manager.command-install
  (:require [clj-yaml.core :as yaml]
            [clojure.java.io :as io]
            [clojure.tools.logging :as log]
            [spekl-package-manager.util :as util]
            [spekl-package-manager.constants :as constants]
            [spekl-package-manager.download :as download]
            ))

(defn read-local-conf
  [file-name]
  (yaml/parse-string (slurp file-name))
  )

(defn read-remote-conf
  ([package version] (read-local-conf (.getFile (io/resource (str "packages/" package "-" version ".yml")))))
  ([package] (read-local-conf (.getFile (io/resource (str "packages/" package ".yml")))))
  )


(defn accuire-remote-package [package version]
  (log/info "[command-install] Finding package" package "in remote repository")
  ;; TODO replace with remote file reading.
  (case version
    '()   (read-remote-conf package)
    (read-remote-conf package version)
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
      


(defn get-my-platform []
  (let [sys (System/getProperty "os.name")]
    (if (.contains sys "Windows")
      "windows"
      (if (.contains sys "Mac")
        "osx"
        (if (.contains sys "Linux")
          "linux"
          "other")))))

(defn is-one-of? [o]
  (not (= nil (o :one-of))))

(defn my-platform? [o]
  (or (= get-my-platform (o :platform)) (= (o :platform "all"))))

(defn is-dep? [o] (and (not (is-one-of? o)) (my-platform? o)))


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
  (.exists (io/as-file (str (constants/package-directory) package "-" version))))
  
(defn gather-deps [package-description]
  (let [package-depends  (package-description :depends)]
    (let [flat-deps  (filter (fn [x] (is-dep? x)) package-depends) configured-deps (gather-one-ofs (filter (fn [x] (is-one-of? x)) package-depends))]
    { 
     :flat-deps flat-deps
     :configured-deps configured-deps
     :all-deps (concat flat-deps configured-deps)
     }
    )))

(defn gather-missing-deps [deps]
  (filter (fn [x] (not (installed? (x :package) (x :version)))) deps))

(defn extract-assets [package-description]
  (let [assets (package-description :assets)]
    (filter (fn [x] (my-platform? x)) assets)))

(defn package-name [package-description]
  (if (= nil (package-description :name))
         (package-description :package)
         (package-description :name)))

(defn package-name-version [package-description]
  (str (package-name package-description) " (version: " (package-description :version) ")"))

(defn print-missing-deps [deps]
  (log/info "[command-install] Will install the following missing packages:")
  (doseq [x deps] (log/info "[command-install] - " (package-name-version x))))


(defn make-package-path [package-description]
  (.getPath (io/file (constants/package-directory) (str (package-name package-description) (package-description :version))))
                     )
(defn make-asset-path [package asset]
  (.getPath (io/file (constants/package-directory) (str package "-" asset)))
  )


(defn create-needed-dirs [package-description]
  (do
    (util/create-dir-if-not-exists (constants/package-directory))
    (util/create-dir-if-not-exists (make-package-path package-description))
    )
  )

(defn install-package [package-description]
  (log/info "[command-install] Starting install of package" (package-name-version package-description))

  (do
    ;; Step 0: install anything it depends on (these will always fetch from remote)
    (log/info "[command-install] Examining dependencies...")
    (let [deps (gather-deps package-description)]
      (let [missing-deps (gather-missing-deps (deps :all-deps))]
        (if (>  (count missing-deps) 0)
          (do
            (print-missing-deps missing-deps)
            (doseq [pkg missing-deps] (install-package (accuire-remote-package (pkg :package) (pkg :version))))
            ))
        ))
    
    (log/info "[command-install] Installing package" (package-name-version package-description))

    ;; Step 1: Directories. Create the .spm and package directory where we will do our work
    (util/create-dir-if-not-exists (constants/package-directory))

    (util/create-dir-if-not-exists (constants/package-directory))
 

    (log/info "[command-install] Downloading Required Assets... ")

    ;; Step 2: Download all the required assets
    (log/info (let [assets (extract-assets package-description)]
      (map (fn [x]
             {
              :local-file  (download/download-to (x :name) (x :url) (make-asset-path (package-name package-description) (x :asset)))
              :symbol-name (x :asset)
              })
           assets)
      ))

    ;; Step 3: Run the installation commands
    ;;
    ;; Baked into the assumptions for these commands is the following:
    ;;
    ;;
    ;; 1) All assets will be located in a directory .spm/<package-name>-<asset-name>/
    ;; 2) The current working directory of every command will be .spm/<package-name>/
    ;; 3) After installation, all files downloaded will be deleted.
    ;; 4)

    )
  
  )


(defn run [arguments options]
  (let [what (first arguments)]
  (case what
    nil (install-package (accuire-local-package))                        ;; install using the package.yml in the current working directory
    (install-package (accuire-remote-package what (rest arguments)))     ;; download the package description and install it.
  )))





;;(> (count (gather-missing-deps ((gather-deps (accuire-remote-package "openjml" nil)) :all-deps))) 1)



;(let [package-description (accuire-remote-package "openjml" '())]
;  (let [downloads (map (fn [x] {
;                                :local-file (download/download-to (x :name) (x :url) (make-asset-path (package-name package-description) (x :asset)))
;                                :symbol-name (x :asset)
;                                } ) (extract-assets package-description))]
;     downloads
;   ))

  

;(= '() null)
