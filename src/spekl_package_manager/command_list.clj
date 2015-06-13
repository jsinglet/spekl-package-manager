(ns spekl-package-manager.command-list
  (:require [spekl-package-manager.net :as net]))


(defn run-specs [packages]
  (doseq [x packages] (println
                       (format "(%s) %-10s - %s" (x "kind") (x "name")  (x "description")))))

(defn run-tools [packages]
  (doseq [x packages] (println
                       (format "(%s)  %-10s - %s" (x "kind") (x "name")  (x "description")))))


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


