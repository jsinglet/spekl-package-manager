(ns spekl-package-manager.runtime)

(def cwd "")

(defn working-dir [] cwd)

(defn set-working-directory [dir]
  (if (= dir ".")
    (def cwd "")
    (def cwd dir)))


