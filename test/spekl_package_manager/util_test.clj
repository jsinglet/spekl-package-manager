(ns spekl-package-manager.util-test
  (:require [spekl-package-manager.util :refer :all]
            [spekl-package-manager.package :as package]
            [clojure.test :refer :all]))


(def local-env-map

  '(
    {:symbol-name "TEST1" :local-file "test"}
    {:symbol-name "TEST2" :local-file "test2"}
    )

  )

(deftest command-to-spm-cmd-test1
  (testing "Basic test 1"
    (is (= (command-to-spm-cmd local-env-map "this is a TEST1") "this is a test"))
    (is (= (command-to-spm-cmd local-env-map "this is a TEST2") "this is a test2"))
    (is (= (command-to-spm-cmd local-env-map "this is a TEST2 TEST1") "this is a test2 test"))
    ))

