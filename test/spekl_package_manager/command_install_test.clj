(ns spekl-package-manager.command-install-test
  (:require [spekl-package-manager.command-install :refer :all]
            [clojure.test :refer :all]))

(deftest can-be-package-or-name
  (testing "Can be a package or name attribute"
    (is (= (package-name (accuire-remote-package "openjml" nil)) "openjml"))
    )
  )



