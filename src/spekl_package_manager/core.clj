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
            )

  (:import jline.Terminal)

  
  (:gen-class))


(declare run)


(defn -main
  [& args]
  (run args))


(defn usage [options-summary]
  (->> ["spm: a tool for writing and using program specifications."
        ""
        "Usage: spm [options] action"
        ""
        "Options:"
        options-summary
        ""
        "Actions:"
        "  install  Install any required specs and tools for this project"
        "  add      Adds specs or tools to this project                  "
        "  remove   Removes specs or tools from this project             "
        "  init     Create a new spekl file (specs | tool)"
        "  list     Print some information about this project (tools | specs)"
        "  check    Run the configured tools on the current project"
        "  find     Searches spekl for specification libraries that might work for your current project"
        "  update   Refresh the specifications attached to this project"
        "  publish  Contribute back the specs you've written for this project."
        ""
        "For more information please see http://spekl.org/docs."]
       (string/join \newline)))


(defn not-implemented [] (println "Not Implemented Yet!"))

(def spekl-options
  ;; An option with a required argument
  [["-p" "--port PORT" "Port number"
    :default 80
    :parse-fn #(Integer/parseInt %)
    :validate [#(< 0 % 0x10000) "Must be a number between 0 and 65536"]]
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

(defn run [args]
  ;; TODO: install, info, check, update, publish
  (let [{:keys [options arguments errors summary]} (parse-opts args spekl-options)]
    (case (first arguments)
      "init"    (command-init/run (rest arguments))
      "install" (not-implemented)
      (exit 1 (usage summary))
    )))


