(ns spekl-package-manager.backend
  (:require [clj-jgit.porcelain :as git]
            [clojure.java.io :as io]
            [clojure.tools.logging :as log]
            [spekl-package-manager.constants :as constants]
            [spekl-package-manager.package :as package]
            )
  (:import [java.io FileNotFoundException File]
           [org.eclipse.jgit.lib RepositoryBuilder AnyObjectId]
           [org.eclipse.jgit.api Git InitCommand StatusCommand AddCommand
                                 ListBranchCommand PullCommand MergeCommand LogCommand
                                 LsRemoteCommand Status ResetCommand$ResetType
                                 FetchCommand]
           [org.eclipse.jgit.submodule SubmoduleWalk]
           [com.jcraft.jsch Session JSch]
           [org.eclipse.jgit.transport FetchResult JschConfigSessionFactory
                                       OpenSshConfig$Host SshSessionFactory]
           [org.eclipse.jgit.util FS]
           [org.eclipse.jgit.merge MergeStrategy]
           [clojure.lang Keyword]
           [java.util List]
           [org.eclipse.jgit.api.errors JGitInternalException]
           [org.eclipse.jgit.transport UsernamePasswordCredentialsProvider]
           [org.eclipse.jgit.treewalk TreeWalk])
  )

;;
;; where all the repos live
;; 
(def repo-homes "../spm-root/")

;;    

(defn extract-commit-message [path]
  (let [package-description (package/package-description-from-path path)]
    (str "Updates to " (package-description :name) " version " (package-description :version))))




(defn pick-author [authors]
  (if (> (count authors) 1) ;; more than one author
   (do
     (println "This package has multiple authors defined. Please select the author you'd like to commit as:")
     (doseq [x (map vector (range 0 (count authors)) authors)]

       (let [idx (x 0) data (x 1)]
         (println (format "%d) %s - %s" idx (data :name) (data :email)))))
     (print "[default: 0] ")
     (flush)
     (let [v (read-line)]
       (try
         (let [idx (read-string v)]
           (if (.contains (range 0 (count authors)) idx)
             (nth authors idx) ;; recursion!
             (pick-author authors)
             ))
         (catch Exception e (pick-author authors)))))
   (if (= 1 (count authors))
    (first authors))))



(defn extract-author-info [path]
  (let [author (pick-author ((package/package-description-from-path path) :author))]
    author))

(defn init []
  (log/info "[backend-init] Creating SPM repository connection...")
  (git/git-init))

(defn init-at [here]
  (log/info "[backend-init-at] Creating SPM repository connection...")
  (git/git-init here))


(defn save-version [path]
  (do
    (git/git-add (git/load-repo path) ".")
    (git/git-add-and-commit (git/load-repo path) (extract-commit-message path) (extract-author-info path)))

  )

(defn is-initial-version? [path]
  (= nil (git/git-branch-list (git/load-repo path))))


(defn get-versions-full [path]
  (map (fn [ref] (.getName ref)) (git/git-branch-list (git/load-repo path)))
  )

(defn get-versions [path]
  (let [versions (get-versions-full path)]
    (map (fn [ref] 
    (let [parts (.split ref "/")]
      (nth parts (- (count parts) 1))
      )) versions)
    ))

(defn new-version [path version]
  (do 
    (git/git-branch-create (git/load-repo path) version)
    (git/git-checkout (git/load-repo path) version)
    ))

(defn calculate-remote [path]
  (let [package-description (package/package-description-from-path path)]
    (str (constants/git-base) (package-description :name) ".git")))

;;
;; Implement some missing command sin clj-git
;;

(defn git-push
  ([^Git repo] (-> repo
                   (.push)
                   (.call)))
  ([^Git repo ^String remote] (-> repo
                                  (.push)
                                  (.setRemote remote)
                                  (.setCredentialsProvider git/*credentials*)
                                  (.call))))

(defn git-pull
  ([^Git repo] (-> repo
                   (.pull)
                   (.call)))
  ([^Git repo ^String remote] (-> repo
                                  (.pull)
                                  (.setRemote remote)
                                  (.call))))





(defn get-password [username]
  (let [msg (str "Password for <" username ">:") ]
  (do
    (print msg)
    (flush)
    (let [pwd (read-line)]
      pwd)
    )))
    ;; (try 
    ;;   (String/valueOf (.readPassword (System/console) msg nil))
    ;;   (catch NullPointerException e (
    ;;                                  (do
    ;;                                    (print msg)
    ;;                                    (flush)
    ;;                                    (let [pwd (read-line)]
    ;;                                      pwd)
    ;;                                    )
    ;;                                  )))))


(defn push-version [path]
  ;; we need a username and password - so first find the author
  (let [author (extract-author-info path)]
    ;; now get the password for the author
    (let [password (get-password (author :email))]
      (git/with-credentials (author :email) password
        (git-push (git/load-repo path) (calculate-remote path)))
      )))

(defn sync-version [path]
  ;; cleanup commits
  (do
    (save-version path) ;; do the local commit
    (push-version path) ;; push it up
    )
  )


