(ns spekl-package-manager.templates
  (:require [clojure.java.io :as io])
  (:import (freemarker.template Configuration
                                Template
                                TemplateException)
           (java.io StringReader
                    StringWriter)
           ))
;;
;; needed to configure freemarker
;;
(defonce ftl-configuration (Configuration.))

(defn load-ftl-resource
  "Gets the template data from the resources directory for the given file"
  [template]
  (let [template-name (str template ".ftl")]
    (if (.exists (io/as-file template))
      (slurp template)
      (slurp (io/resource template-name)))))

(defn load-template [template]
  (Template. "name" (StringReader. (load-ftl-resource template)) ftl-configuration))

(defn template-with-params [template params]
  (let [writer (StringWriter.), t (load-template template)]
    (do
      (.process t params writer)
      (.toString writer)
      )))




