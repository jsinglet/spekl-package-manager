(ns spekl-package-manager.backend
  (:require [clj-jgit.porcelain :as git]
            [clojure.java.io :as io]
            [clojure.tools.logging :as log]
            [spekl-package-manager.constants :as constants]
            [spekl-package-manager.package :as package]
            [clj-http.client :as client]
            [clojure.java.shell :as shell]
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
(declare is-initial-version?)
(declare calculate-remote)

(defn extract-commit-message [path]
  (if (is-initial-version? path)
    "Creation of package"
    (let [package-description (package/package-description-from-path path)]
      (str "Updates to " (package-description :name) " version " (package-description :version))))
  )




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


(defn save-version [path]
  (do
    (git/git-add (git/load-repo path) ".")
    (git/git-add-and-commit (git/load-repo path) (extract-commit-message path) (extract-author-info path)))

  )

(defn init []
  (do
    (log/info "[backend-init] Creating SPM repository connection...")
    (git/git-init)
    (save-version ".")
    ))

(defn init-at [here]
  (do
    (log/info "[backend-init-at] Creating SPM repository connection...")
    (git/git-init here)
    (save-version here)))



(defn git-tag-list
  ([^Git repo]
   (-> repo
       (.tagList)
       (.call)))
   ([^Git repo ^String remote]
    (-> repo
        (.lsRemote)
        (.setRemote remote)
        (.setTags true)
        (.setHeads false)
        (.call)
        )
     )
  )

(defn get-versions-full [path]
 (concat 
   (map (fn [ref] (.getName ref)) (git-tag-list (git/load-repo path)))   ;; locals
   (map (fn [ref] (.getName ref)) (git-tag-list (git/load-repo path) (calculate-remote path))) ;; remotes
  )
  )

(defn get-versions [path]
  (distinct (let [versions (get-versions-full path)]
     (map (fn [ref] 
            (let [parts (.split ref "/")]
              (nth parts (- (count parts) 1)))
            ) versions)
     )))



(defn git-tag [^Git repo ^String tag]
  (-> repo
      (.tag)
      (.setName tag)
      (.call)))

(defn git-delete-tag [^Git repo ^String tag]
  (-> repo
      (.tagDelete)
      (.setTags (into-array String [tag]) )
      (.call)
      )
  )

(defn is-initial-version? [path]
  (= 0 (count (git-tag-list (git/load-repo path)))))

(defn new-version [path version]
  (do
    (save-version path)                     ;; do the local commit
    (git-tag (git/load-repo path) version)
    ;; (git/git-branch-create (git/load-repo path) version)
    ;; (git/git-checkout (git/load-repo path) version)
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
                   (.setPushAll)
                   (.setPushTags)
                   (.call)))
  ([^Git repo ^String remote] (-> repo
                                  (.push)
                                  (.setPushAll)
                                  (.setPushTags)
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


    ;; (do
  ;;   (print msg)
  ;;   (flush)
  ;;   (let [pwd (read-line)]
  ;;     pwd)
  ;;   )))
    (try 
      (String/valueOf (.readPassword (System/console) msg nil))
      (catch NullPointerException e (
                                     (do
                                       (print msg)
                                       (flush)
                                       (let [pwd (read-line)]
                                         pwd)
                                       )
                                     )))))


(defn push-version [path]
  ;; we need a username and password - so first find the author
  (let [author (extract-author-info path)]
    ;; now get the password for the author
    (let [password (get-password (author :email))]
      (git/with-credentials (author :email) password
        (git-push (git/load-repo path) (calculate-remote path)))
      )))



(defn undo-version [path]
  (let [version ((package/package-description-from-path path) :version)]
    (git-delete-tag (git/load-repo path) version)
    ))

(defn sync-version [path]
  ;; cleanup commits
  (do
    ;; if we are at this point, we have created a tag that matches the current version. if this fails, we need to delete that tag.
    (try
      (push-version path)
      (catch Exception e   (log/info "[command-publish] Publish failed with message: " (.getMessage e) ". Rolling back this version.") (undo-version path))
      ) ;; push it up
    )
  )

(defn register-project [name username]
  (client/get (str constants/spm-home "Package/create" ) {:query-params {"project" name "username" username}}))



(defn install-package [package-description]
  (do
    ;; copy it down
    (shell/sh "git" "clone" (package/create-package-base-url package-description) (package/make-package-path package-description))
    ;; this command has problems in certain shell enviroments. the shell command is a workaround for the moment. 
    ;(git/git-clone-full (package/create-package-base-url package-description)  (package/make-package-path package-description))

    ;; switch to the correct version
    (git/git-checkout (git/load-repo (package/make-package-path package-description)) (package-description :version))

    )
  )

