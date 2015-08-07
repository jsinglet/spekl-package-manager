(ns spekl-package-manager.net
  (:require [clojure.data.json :as json]
            [clojure.java.io :as io]
            [clj-http.client :as client]
            [spekl-package-manager.constants :as constants]
            [spekl-package-manager.versioning :as version]
            )
  (:import
   (java.util.ArrayList)
   (org.apache.maven.artifact.versioning ComparableVersion)

   (org.spekl.spm.utils PackageLoadException
                         ProjectConfigurationException
                         CantFindPackageException
                         )
   ))

(defn fetch-url [address]
  (with-open [stream (.openStream (java.net.URL. address))]
    (let  [buf (java.io.BufferedReader. 
                (java.io.InputStreamReader. stream))]
      (apply str (line-seq buf)))))


(defn load-packages [subset]
  (let [all (json/read-str ((client/get (constants/spm-package-list)) :body))]
    (case subset
      :all all
      :specs (filter (fn [x] (= (x "kind") "specs")) all)
      :tools (filter (fn [x] (= (x "kind") "tool")) all)
      all)))


(defn find-satisfying-package [name version]
  (let [packages (load-packages :all)]

    ;; find the ones that satisfy...
    (let [satisfying (filter (fn [x] (and (.equalsIgnoreCase name (x "name")) (version/version-satisfies (x "version") version)   )) packages)]

      (if (= 0 (count satisfying))
        (throw (CantFindPackageException. (str "Unable to find package: " name " satisfying version: " version))   )

        ;; return the newest version

        (first (reverse (sort
                         (fn [x y] (.compareTo (ComparableVersion. (x "version"))  (ComparableVersion. (y "version"))))
                         satisfying
                         )))))))


