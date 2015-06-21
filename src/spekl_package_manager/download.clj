(ns spekl-package-manager.download
  (:import (org.spekl.spm.utils ProgressableFileDownload)))


(defn download-to [label url where]
  (let [the-file (ProgressableFileDownload. label url where )]
    (.download the-file)
    ))
