(ns spekl-package-manager.versioning-test
  (:require [spekl-package-manager.versioning :refer :all]
            [clojure.test :refer :all]))



(deftest operators
  (testing "All Ops"
    (is ((translate-expression "> 1.1.1") "1.1.2"))
    (is ((translate-expression "> 1.1.1") "2.0"))

    (is ((translate-expression "< 1.1.1") "1.0"))
    (is ((translate-expression "< 1.1.1") "1.1.0"))

    (is ((translate-expression "<= 1.1.1") "1.0"))
    (is ((translate-expression "<= 1.1.1") "1.1.0"))
    (is ((translate-expression "<= 1.1.1") "1.1.1"))

    (is ((translate-expression ">= 1.1.1") "1.1.2"))
    (is ((translate-expression ">= 1.1.1") "1.2"))
    (is ((translate-expression ">= 1.1.1") "1.1.1"))

)
  )


(deftest parsing
  (testing "All Ops"
    (is (version-satisfies "1.0" "1.0"))
    (is (version-satisfies "1.0" "= 1.0"))

    (is (version-satisfies "1.0" " >= 1.0"))
    (is (version-satisfies "1.0" " > 0.1"))
    (is (version-satisfies "1.0" "< 2.1.0"))
    (is (version-satisfies "1.0" "<= 2.1.0"))

    (is (version-satisfies "1.0" ">= 1.0 && <= 2.0"))
    (is (version-satisfies "1.1" ">= 1.0 && <= 2.0"))
    (is (version-satisfies "1.1.1" ">= 1.0 && <= 2.0"))
    (is (version-satisfies "1.9" ">= 1.0 && <= 2.0"))



    (is (version-satisfies "1.9" "= 1.9 || <= 2.0"))
    (is (version-satisfies ".9" "= 1.9 || <= 1.0"))

    
    )
  )
