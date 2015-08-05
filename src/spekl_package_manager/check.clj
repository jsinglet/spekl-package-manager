(ns spekl-package-manager.check
  (:require
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
  )



;; (defn defcheck [a]
;;   a
;;   )

;; (defcheck "tset")


;; (defcheck 
;;   (run "java -jar ${openjml:openjml.jar} -esc ${project-files-string}")
;;   (run-suppress-output "")
;;   (run "java" "-jar" ("openjml" "openjml.jar") ("openjml.properties"))
;;   (last-exit-code "You can't do stuff.")
;;   (% "This is some decorated output")
;;   (stderr)
;;   (rm ".sh")
;;   (cp "" "")q
;;   )

;; (defcheck default

  
;;   )

;(expand)
;;(run "java" "-jar" "${}")

(def ^:dynamic *resolver* nil)
(def ^:dynamic *project-files-string* nil)
(def ^:dynamic *project-files* nil)
(def ^:dynamic *specs* nil)


(defmacro defcheck
  [name & body]
  `(defn ~name [] (do ~@body))
  )

(defn resolve-path
  ([asset]  (*resolver* asset))
  ([package asset] (*resolver* package asset))
  )

(defn is-special? [what]
  (and (.startsWith what "${") (.endsWith what "}")))

(defn do-expand [what]
  (let [to-expand (first (rest (re-matches #"\$\{(.*)\}" what)))]
    (let [parts (.split to-expand ":")]
      (if (= 1 (count parts))
        (resolve-path (first parts))
        (resolve-path (first parts) (last parts))))))


(defn expand [what]
  (if (is-special? what)
    (do-expand what)
    what))

(defn stream-result [res]
  (do
    (println (res :out))
    (println (res :err))
    res
    )
  )

(defn run [& args]
  (let [runargs (doall (map (fn [x] (expand x)) args))]
    (stream-result (apply shell/sh runargs))))


(declare default)

