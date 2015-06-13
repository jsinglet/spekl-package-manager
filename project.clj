(defproject spekl-package-manager "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [clj-yaml "0.4.0"]
                 [org.clojure/tools.cli "0.3.1"]
                 [org.freemarker/freemarker "2.3.20"]
                 [org.clojure/data.json "0.2.6"]
                 [org.clojure/tools.logging "0.3.1"]
                 [clj-http "1.1.2"]
                 [org.slf4j/slf4j-log4j12 "1.7.1"]
                 [log4j/log4j "1.2.17" :exclusions [javax.mail/mail
                                                    javax.jms/jms
                                                    com.sun.jmdk/jmxtools
                                                    com.sun.jmx/jmxri]]
                 ]
  :main ^:skip-aot spekl-package-manager.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
