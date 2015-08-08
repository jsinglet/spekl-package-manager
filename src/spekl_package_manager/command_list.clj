(ns spekl-package-manager.command-list
  (:require [spekl-package-manager.net :as net]
            [clojure.core.reducers :as r]
            [clojure.string :as string]
            
            ))



(def nl "\n                                                   ")

(defn block-format-string [ls len]
  (let [ss (into [] (seq ls))]
    (string/join "" (flatten (r/reduce (fn [acc x]
                          (if (and (> (count acc) 0 ) (= 0 (mod (count acc) len)))
                            (conj acc [nl x])
                            (conj acc x))) [] ss)))))

(defn print-listing [packages]
  (doseq [x packages] (println
                       (format "(%s) %-20s %-20s - %s " (x "kind") (x "name")  (format "(v%s)" (x "version")) (block-format-string (x "description") 80)))))


(defn run-list []
  (do
    (print-listing (net/load-packages :tools))
    (print-listing (net/load-packages :specs))
    ))


(defn run [arguments]
  (case (first arguments)
    "specs" (print-listing (net/load-packages :specs))
    "tools" (print-listing (net/load-packages :tools))
    "all"   (run-list)
    nil     (run-list)
    (throw (IllegalArgumentException. "Invalid selection"))))




