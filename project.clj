(defproject spekl-package-manager "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [clj-yaml "0.4.0"]
                 [org.clojure/tools.cli "0.3.1"]
                 [org.freemarker/freemarker "2.3.20"]
                 [org.clojure/tools.logging "0.3.1"]
                 [jline "0.9.94"]
                 ]
  :main ^:skip-aot spekl-package-manager.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
