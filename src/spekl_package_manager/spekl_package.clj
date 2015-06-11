(ns spekl-package-manager.spekl-package)

(require '[clj-yaml.core :as yaml])


(defn read-conf
  [file-name]
  (yaml/parse-string (slurp file-name))
  )

(yaml/parse-string  "
- {name: John Smith, age: 33}
- name: Mary Smith
  age: 27
")
