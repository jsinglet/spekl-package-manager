(ns spekl-package-manager.command-list
  (:require [spekl-package-manager.net :as net]

            ))


(defn run-specs [packages]
  (doseq [x packages] (println
                       (format "(%s) %-20s - %s (version: %s)" (x "kind") (x "name")  (x "description")  (x "version")))))
 
(defn run-tools [packages]
  (doseq [x packages] (println
                       (format "(%s)  %-20s - %s (version: %s)" (x "kind") (x "name")  (x "description")  (x "version")))))


(defn run-list []
  (do
    (run-tools (net/load-packages :tools))
    (run-specs (net/load-packages :specs))
    ))


(defn run [arguments]
  (case (first arguments)
    "specs" (run-specs (net/load-packages :specs))
    "tools" (run-tools (net/load-packages :tools))
    "all"   (run-list)
    nil     (run-list)
    (throw (IllegalArgumentException. "Invalid selection"))))


