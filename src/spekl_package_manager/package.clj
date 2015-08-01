(ns spekl-package-manager.package
  (:require [clj-yaml.core :as yaml]
            [clojure.java.io :as io]
            [clojure.tools.logging :as log]
            [spekl-package-manager.util :as util]
            [spekl-package-manager.constants :as constants]
            )
  (:import
    (java.io FileNotFoundException)
    (org.spekl.spm.utils PackageLoadException)))


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

(defn accuire-remote-package [package version]
  (log/info "[command-install] Finding package" package "in remote repository")
  ;; TODO replace with remote file reading.
  (case version
    '()   (read-remote-conf package)
    (if (instance? String version)
      (read-remote-conf package version)
      (read-remote-conf package (first version))
      )

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
