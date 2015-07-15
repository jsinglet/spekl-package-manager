(ns spekl-package-manager.package-test
  (:require [spekl-package-manager.package :refer :all]
            [clojure.test :refer :all]))

(deftest can-be-package-or-name
  (testing "Can be a package or name attribute"
    (is (= (package-name (accuire-remote-package "openjml" '())) "openjml"))
    )
  )


(deftest should-be-like-two-commands-here
  (testing "We should have two commands here."
    (is (= 2 (count (gather-install-commands (accuire-remote-package "openjml" '()))))))
  )
