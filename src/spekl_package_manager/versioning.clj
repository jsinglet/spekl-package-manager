(ns spekl-package-manager.versioning
  (:require [clj-yaml.core :as yaml]
            [clojure.java.io :as io]
            [clojure.tools.logging :as log]
            [spekl-package-manager.util :as util]
            [spekl-package-manager.constants :as constants]
            [clojure.core.reducers :as r]
            [clojure.string :as string]
            )
  (:import
   (java.io FileNotFoundException)
   (org.apache.maven.artifact.versioning ComparableVersion) 
    (org.spekl.spm.utils PackageLoadException
                         ProjectConfigurationException
                         CantFindPackageException
;                         InvalidVersionException
                         ))
  )

; version: 1.1.0
; condition: atleast  

;;
;; Semantic version resolution
;;
;; Version strings can be things like
;;
;; - symbolic references: latest, > 1.1.0 && < 1.1.2
;; - concrete references: 1.1.1
;; - boolean expressions:  > 1.1.0
;; - more boolean       :  > 1.1.0 && < 1.1.2
;;
;; In all cases, the default is always to pick the NEWEST vesrion of a package that will satisfy the dependencies. 
;;

(defn semantic-version-predicate-kind []

  ;; supports the following
  ;;
  ;; 1.1.1   - literal versions
  ;; > 1.1.1 - simple boolean predicates 
  

  )



;; we transform these expressions into s-expressions

;; eg 1.1.1

;; simple versions - eg: 1.1.1
;;(fn [x] (= 111 x))

;; simple boolean expressions - eg: > 1.1.1
;;(fn [x] (> x  1))

;; more complex expressions   - eg: > 1.1.1 && < 2.0 || (> 2.0 && < 3.0)
;;(fn [x] )

(defn is-naked-version [ver]
  (= (count (filter (fn [x] (.contains ver x)) (list ">" "<" "=" "&" "|"))) 0))



;; creates a predicate taking a single argument that returns true if the
;; argument satisfies the predicate
(defn to-bexp [op s-ver]
  (let [ver (ComparableVersion. s-ver)]
    (fn [x]
      (let [test-version (ComparableVersion. x)]
        (case op
          "<" (= -1 (.compareTo test-version ver))
          ">" (= 1 (.compareTo test-version ver))
          "=" (= 0 (.compareTo test-version ver))
          "==" (= 0 (.compareTo test-version ver))
          "<=" (or (= -1 (.compareTo test-version ver)) (= 0 (.compareTo test-version ver)))
          ">=" (or  (= 1 (.compareTo test-version ver)) (= 0 (.compareTo test-version ver)))
          (throw (IllegalArgumentException. (str "Unknown operator: " op)) ))))))


(defn to-cexp [op lhs rhs]
  (fn [x]
    (cond
      (= op 'sym-and) (and (lhs x) (rhs x))
      (= op 'sym-or)  (or (lhs x)  (rhs x))
      :else (throw (IllegalArgumentException. (str "Unknown logical connector: " op)) )
      )
    ))

(defn translate-expression [str]
  (let [parts (.split str " ")]
    (to-bexp (first parts) (last parts))
    )
  )



(defn get-tokens [version-string]
  (filter (fn [x] (not (= "" x))) (string/split version-string #" ")))


(defn is-boolean-op? [x]
  (util/in? '("<" ">" "<=" ">=", "=", "==") x)
  )
(defn is-boolean-con? [x]
  (util/in? '("&&" "||") x))

(defn to-sym [x]
  (case x
    "&&" 'sym-and
    "||" 'sym-or
    (throw (Exception. (str "Invalid token: " x)))))

(defn tokenize [tokens]
  (let [current-token (first tokens)]

    (cond
      (is-boolean-op? current-token)  (conj (tokenize (drop 2 tokens)) (list 'bexp (list current-token (nth tokens 1))))
      (is-boolean-con? current-token) (conj (tokenize (rest tokens)) (list (to-sym current-token)))
      (= nil current-token) '()
      :else (throw (Exception. (str "Invalid token: " current-token)))
      )
    )
  )

(defn l-to-bexp [l]
  (to-bexp (first l) (last l)))

(defn parse [tokens stack]
  (let [current-token (first tokens)]
   (cond
     (= (first current-token) 'bexp)                                           (parse (rest tokens) (conj stack (l-to-bexp (rest (flatten current-token)))))
     (or (= (first current-token) 'sym-or) (= (first current-token) 'sym-and)) (parse
                                                                                (drop 2 tokens) ;; the tokens stating after the NEXT token
                                                                                (conj
                                                                                 (rest stack)   ;; get rid of the last element we put on the stack
                                                                                 (to-cexp
                                                                                  (first current-token) ;; the connector
                                                                                  (first stack)         ;; the last thing we put on the stack (a;ready parsed)
                                                                                  (l-to-bexp (rest (flatten (first  (rest tokens))))) ;; the next logical expression that we will parse now
                                                                                  
                                                                                  )) )
     (= nil current-token) stack
     :else (throw (Exception. (str "Parse error on version number: " current-token)))
     ))
  
  
  )

(defn version-satisfies [try-version required-version-string]
  (try 
    ((first (parse (tokenize (get-tokens required-version-string)) '())) try-version)
    ;; fall back to exact comparison
    (catch Exception e (.equalsIgnoreCase try-version required-version-string))
    ))




