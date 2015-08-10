;;; SPM --- The Spekl Package Manager 

;; Copyright (C) 2015 John L. Singleton <jls@cs.ucf.edu>

;; Author: John L. Singleton <jls@cs.ucf.edu>
;; Version: 20150519
;; URL: http://www.emacswiki.org/emacs/LaTeXPreviewPane

;;; Commentary:

;; The Spekl Package manager is a tool that makes it easier to write and use specifications.

;; Modes of Operation:
;;
;; usage: spm [check|publish|refresh] 


;;; Code:

(ns spekl-package-manager.core
  (:require [clojure.tools.cli :refer [parse-opts]]
            [clojure.string :as string]
            [spekl-package-manager.command-init :as command-init]
            [spekl-package-manager.command-list :as command-list]
            [spekl-package-manager.command-install :as command-install]
            [spekl-package-manager.command-publish :as command-publish]
            [spekl-package-manager.command-cache :as command-cache]
            [spekl-package-manager.command-check :as command-check]

            [clojure.java.io :as io]
            [spekl-package-manager.runtime :as rt]
            [spekl-package-manager.progress :as pr]

            )
  (:gen-class))


(declare run)


(defn -main
  [& args]
  (run args))

(defn usage [options-summary]
  (->> [
        (slurp  (io/resource "banner.txt"))
        "spm: a tool for writing and using program specifications."
        ""
        "Usage: spm [options] action"
        ""
        "Options:"
        options-summary
        ""
        "General Actions:"
        "  install  Install any required specs and tools for this project. You may specify "
        "           an optional argument that is a package name or a package name and a version"
        "           of the package to install."
        "  help     Displays this message"
        ""
        "Check-Related Actions:"
        "  check    Run the configured tools on the current project. The checks will be run using the "
        "           configured checks in the spekl.yml file. If an argument to this command is supplied, then"
        "           only the named check will be run."
        ""
        "Specification-Related Actions:"
        "  refresh  Checks the central repository for updates to any atttached specifications. This process "
        "           normally happens during the checking of your project, however it can be invoked manually"
        "           using this command."
        "  find     Searches spekl for specification libraries that might work for your current project"
        ""
        "Project-Related Actions:"
        "  init     Create a new Spekl project. May be one of: "
        "             project     Creates a new project that you can attach specs and checks to (default)"
        "             tool        Creates a new project for building a Spekl tool/check"
        "             specs       Creates a new project for writing a Spekl specification library."
        "  extend   Create a new Spekl Project that extends another project. "
        ""
        "Package Management Actions:"
        "  list     Displays a list of available tools and specification libraries. May be one of:"
        "             all         Displays a list of all specs and tools in one listing (default)"
        "             specs       Displays a list of available specification libraries."
        "             tools       Displays a list of available verification tools (checks)."
        ""
        "  publish  Contribute back the current tool or specification project."
        ""
        "For more information please see http://spekl-project.org/docs."]
       (string/join \newline)))


(defn not-implemented [] (println "Not Implemented Yet!"))

(def spekl-options
  ;; An option with a required argument
  [
   ["-d" "--dir DIRECTORY" "The effective project directory. Defaults to the current directory."
    :id :directory
    :default "."
    ]

   ;; A non-idempotent option
   ["-v" nil "Verbosity level"
    :id :verbosity
    :default 0
    :assoc-fn (fn [m k _] (update-in m [k] inc))]
   ;; A boolean option defaulting to nil
   ["-h" "--help"]])


(defn exit [status msg]
  (println msg)
  );;(System/exit status))


(defn setup-local-env [options]
  (do
    (rt/set-working-directory (options :directory))
    ))

(defn run [args]
  ;; TODO: install, info, check, update, publish
  (let [{:keys [options arguments errors summary]} (parse-opts args spekl-options)]
    (do
      (setup-local-env options)
      (try 
        (case (first arguments)
          "init"    (command-init/run (rest arguments) options)
          "list"    (command-list/run (rest arguments))
          "silly"    (pr/silly-loop2)
          "install" (command-install/run (rest arguments) options)
          "publish" (command-publish/run (rest arguments) options)
          "cache"   (command-cache/run (rest arguments) options)
          "check"   (command-check/run (rest arguments) options)          
          "help"    (exit 0 (usage summary))
          (exit 1 (usage summary)))
        (catch IllegalArgumentException e (exit 1 (usage summary))))
      )))
