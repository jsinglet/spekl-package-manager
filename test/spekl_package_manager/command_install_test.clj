(ns spekl-package-manager.command-install-test
  (:require
    [spekl-package-manager.package :as package]
    [spekl-package-manager.command-install :refer :all]
            [clojure.test :refer :all]))

(deftest can-be-package-or-name
  (testing "Can be a package or name attribute"
    (is (= (package/package-name (package/accuire-remote-package "openjml" '())) "openjml"))
    )
  )



