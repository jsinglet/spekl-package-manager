(ns spekl-package-manager.util
  (:require [clojure.java.io :as io]
            [spekl-package-manager.constants :as constants]))


(defn create-dir-if-not-exists [dir]
  (if (not (.exists (io/as-file dir)))
    (.mkdir (java.io.File. dir))))

