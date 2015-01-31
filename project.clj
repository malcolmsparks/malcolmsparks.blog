(defproject malcolmsparks.blog "0.1.0-SNAPSHOT"
  :description "A modular project created with lein new modular clean-blog"
  :url "http://github.com/malcolmsparks/malcolmsparks.blog"

  :exclusions [com.stuartsierra/component]

  :dependencies
  [
   [clj-time "0.9.0"]
   [endophile "0.1.2"]
   [com.stuartsierra/component "0.2.2"]
   [juxt.modular/bidi "0.7.3"]
   [juxt.modular/clostache "0.6.1"]
   [juxt.modular/http-kit "0.5.3"]
   [juxt.modular/less "0.1.2"]
   [juxt.modular/maker "0.5.0"]
   [juxt.modular/wire-up "0.5.0"]
   [malcolmsparks/co-dependency "0.1.5"]
   [org.clojure/clojure "1.7.0-alpha4"]
   [org.clojure/tools.logging "0.2.6"]
   [org.clojure/tools.reader "0.8.9"]
   [org.slf4j/jcl-over-slf4j "1.7.2"]
   [org.slf4j/jul-to-slf4j "1.7.2"]
   [org.slf4j/log4j-over-slf4j "1.7.2"]
   [org.webjars/bootstrap "3.3.0"]
   [org.webjars/jquery "2.1.0"]
   [prismatic/plumbing "0.3.5"]
   [prismatic/schema "0.3.3"]
   [ch.qos.logback/logback-classic "1.0.7" :exclusions [org.slf4j/slf4j-api]]
   ]

  :main malcolmsparks.blog.main

  :repl-options {:init-ns user
                 :welcome (println "Type (dev) to start")}

  :aliases {"gen" ["run" "-m" "malcolmsparks.blog.generate"]}

  :profiles {:dev {:dependencies [[org.clojure/tools.namespace "0.2.5"]]
                   :source-paths ["dev"
                                  ]}})
