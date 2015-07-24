(ns spekl-package-manager.command-cache
  (:require [clj-jgit.porcelain :as git]
            [clojure.java.io :as io]
            [clojure.tools.logging :as log]
            [spekl-package-manager.util :as util]
            [spekl-package-manager.constants :as constants]
            [spekl-package-manager.package :as package]
            [clj-http.client :as client]
            [spekl-package-manager.net :as net]
            [clojure.data.json :as json]
            [clj-yaml.core :as yaml]
           
            )
  )

;;
;; builds the package cache
;;
(defn json-load-uri [uri]
  (log/info "Loading URL: " uri)
  (json/read-str ((client/get uri
                              {:basic-auth [constants/api-name constants/api-key]}) :body))
  )
(defn is-next-link [part]
  (.contains part "rel=\"next\""))

(defn extract-next-link [header]
  (first (filter
    (fn [x] (is-next-link x)) 
    (.split header ",")
    ))
  )

(defn to-link [header]
  (if (= nil header)
    header
   (let [left (first (.split header ";"))]
     (subs left 1 (- (.length left) 1))
     ))
  
  )
(defn github-make-link [header]
  (if (= nil header)
    header
    ;; try to find a next link
    (to-link (extract-next-link header)))
  )

(defn github-next-url [uri]
  (github-make-link (((client/get uri
                                  {:basic-auth [constants/api-name constants/api-key]}) :headers) "Link")))

(defn json-load-uri-autonext [uri]
  (if (= nil uri)
    '()
    (concat (json-load-uri uri) (json-load-uri-autonext (github-next-url uri)))))


(defn get-repos []
  (json-load-uri-autonext constants/api-all-repos))

(declare package-uris-for-repo)

(defn boil-down [type e]
  (case type
    :all  (flatten e)
    :fast (filter (fn [x] (not (= nil x))) (map (fn [x] (first x)) e))
    )
  )

(defn get-all-package-uris [type]

  (let [repos (get-repos)]
    (boil-down type (map (fn [repo]
            (try
              (log/info "[command-cache] Building package list for repo: " (repo "name"))
              (package-uris-for-repo repo)
              (catch Exception e (log/info "[command-cache] Skipping invalid repo with message:" (.getMessage e)) '())
              )
            )
          repos))
    )

  )

(defn to-listing [package-uri]

  (try 
   (let [conf (package/read-conf ((client/get package-uri
                                              {:basic-auth [constants/api-name constants/api-key]}) :body))]
     {
      :name         (conf :name)
      :version      (conf :version)
      :kind         (conf :kind)
      :description  (conf :description)
      }
     )
   (catch Exception e (log/info "[command-cache] Skipping url due to error: " package-uri (.getMessage e)))))

(defn build-cache [type]
  (let [package-uris (get-all-package-uris type)]
    (let [packages  (filter (fn [e] (not (= nil e))) (map (fn [uri] (to-listing uri)) package-uris))]
      (println (json/write-str packages)))))

(defn create-package-url [name version]
  (str constants/api-raw constants/org-name "/" name "/" version "/" (constants/package-filename)  ))

(defn package-uris-for-repo [repo]
  (let [tag-uri (repo "tags_url") name (repo "name")]
    (let [tags (json-load-uri-autonext tag-uri)]
      ;; for each tag, create a package.yml url
      (map (fn [tag]
             (create-package-url name (tag "name"))
             ) tags)
      )
    )
  )

(defn run [arguments options]
  (log/info "[command-cache]")
  (case (first arguments)
    "full" (build-cache :all)
    "fast"  (build-cache :fast)
    (build-cache :all)))
