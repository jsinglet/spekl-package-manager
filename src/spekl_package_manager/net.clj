(ns spekl-package-manager.net
  (:require [clojure.data.json :as json]
            [clojure.java.io :as io]

            ))

(defn fetch-url [address]
  (with-open [stream (.openStream (java.net.URL. address))]
    (let  [buf (java.io.BufferedReader. 
                (java.io.InputStreamReader. stream))]
      (apply str (line-seq buf)))))


(defn load-packages [subset]
  (let [all (json/read-str (slurp (io/resource "static-package-list.json")))]
    (case subset
      :all all
      :specs (filter (fn [x] (= (x "kind") "specs")) all)
      :tools (filter (fn [x] (= (x "kind") "tool")) all)
      all)))
