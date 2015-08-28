(ns spekl-package-manager.util
  (:require [clojure.java.io :as io]
            [spekl-package-manager.constants :as constants]
            [clojure.string :as str]
            [clojure.java.shell :as shell]
            [clojure.core.reducers :as r]
            [org.satta.glob :as glob]
            [clojure.string :as string]
            ))


(defn create-dir-if-not-exists [dir]
  (if (not (.exists (io/as-file dir)))
    (.mkdir (java.io.File. dir))))


(defn get-my-platform []
  (let [sys (System/getProperty "os.name")]
    (if (.contains sys "Windows")
      "windows"
      (if (.contains sys "Mac")
        "osx"
        (if (.contains sys "Linux")
          "linux"
          "other")))))

;;
;; Takes a list of maps of the form {:local-file, :symbol-name}
;;
(defn command-to-spm-cmd [env cmd]
  ;; replace anything we need from the environment in this command.
  (let [h (first env)]
    (case h
      nil cmd
      (command-to-spm-cmd (rest env) (str/replace cmd (h :symbol-name) (h :local-file)))
      )
    )
  )

(defn execute-command-in-directory [env cmd dir]
  (let [spm-command (command-to-spm-cmd env cmd)]
    (apply shell/sh (reverse (cons dir (cons :dir (into '()  (str/split spm-command  #" ")))))))
  )




(defn in? 
  "true if seq contains elm"
  [seq elm]  
  (some #(= elm %) seq))



(defn expand-glob [globs]
  (let [groups (map (fn [x] (glob/glob (.trim x))) globs)]
    ;; combine
    (map (fn [x] (.getPath x)) (r/reduce (fn [acc x] (concat acc x)) []  groups))
    ))


(defn get-open-command [file]
  (let [platform (get-my-platform)]
    (case platform
      "windows" (list "cmd" "/c" "start" file) 
      "linux"   (list "xdg-open" file)
      "osx"     (list "open" file)
      nil
      )))
