(defproject slack-bot-clj "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [slack-rtm "0.1.6"]
                 [org.clojure/data.json "0.2.6"]
                 [clj-time "0.13.0"]
                 [org.julienxx/clj-slack "0.5.5"]]
  :main ^:skip-aot slack-bot-clj.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
