(ns spekl-package-manager.backend
  (:require [clj-jgit.porcelain :as git]
            [clojure.tools.logging :as log]
            ))

;;
;; where all the repos live
;; 
(def repo-homes "../spm-root/")


(defn init []
  (log/info "[backend-init] Creating SPM repository connection...")
  (git/init))

(defn init-at [here]
  (log/info "[backend-init-at] Creating SPM repository connection...")
  (git/init here))





