(defproject spekl-package-manager "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :resource-paths ["resources/" "resources/packages/"]
  :libdir-path "target/deps"
  :java-source-paths ["src/java"]
  :license {:name "BSD"
            :url "http://opensource.org/licenses/isc-license"}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [clj-yaml "0.4.0"]
                 [org.clojure/tools.cli "0.3.1"]
                 [org.freemarker/freemarker "2.3.20"]
                 [org.clojure/data.json "0.2.6"]
                 [org.clojure/tools.logging "0.3.1"]
                 [clj-http "1.1.2"]
                 [clj-glob "1.0.0"]
                 [net.n01se/clojure-jna "1.0.0"]

                 [clj-jgit "0.8.9"]
                 [org.apache.maven/maven-artifact "3.3.3"]
                 [org.slf4j/slf4j-log4j12 "1.7.1"]
                 [log4j/log4j "1.2.17" :exclusions [javax.mail/mail
                                                    javax.jms/jms
                                                    com.sun.jmdk/jmxtools
                                                    com.sun.jmx/jmxri]]
                 ]

  
  :main ^:skip-aot spekl-package-manager.core
  :target-path "target/%s"
  :profiles {:uberjar {
                       :main spekl-package-manager.core
                       :aot :all
                       :resource-paths ["resources/"]
                       }
             :dist {
                   :main spekl-package-manager.core
                   :aot :all
                   :resource-paths ["resources/"]
                   }
             })
