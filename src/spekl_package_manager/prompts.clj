(ns spekl-package-manager.prompts)

(defrecord Prompt [text default])

;;
;; Available Spekl Tools
;; ======================================================================
;; 1) [x] OpenJML  - A Program Verification tool for Java Programs using JML
;; 2) [ ] SPARTA   - A Security Toolkit
;; 3) [ ] FindBugs - A static analysis toolkit for Java programs
;; ======================================================================
;;
;;
;; spm add openjml-rac
;; spm attach  openjml-core-java 1.1.1
;; spm remove openjml
;; spm detach  openjml-core-java 
;;
;; spm list specs openjml
;; spm list specs sparta
;; spm list tools openjml

(defn to-prompts [prompts]
  "Converts a series of Prompt records to maps that contain text that can be displayed to the user in a prompt
along with a mapped value to use in case the user does not supply a value"
  (map (fn [x] {:text (str (:text x) " " "[default: " (:default x) "] ") :default (:default x)}) prompts))

(defn read-line-or-value [v]
  "Reads a value from the user, otherwise outputs "
  (let [s (read-line)]
    (case s
      "" v
      s)))

(defn looks-reasonable [what if-yes if-no]
  (do
    (println what)
    (flush)
    (print "Does this configuration look reasonable? [Y/n] ")
    (flush)
    (if (.equalsIgnoreCase (read-line) "Y") (if-yes) (if-no))))
            
(defn get-prompt-info [prompts]
    (map (fn [x] (do
                   (print (get x :text))
                   (flush)
                   (read-line-or-value (get x :default))
                   )) (to-prompts prompts)))

(defn create [text default] (Prompt. text default))

(defn fill [ps p] (vec (get-prompt-info (get ps p))))
