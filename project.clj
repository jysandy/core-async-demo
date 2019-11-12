(defproject core-async-demo "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.10.1"]
                 [org.clojure/core.async "0.4.500"]
                 [http-kit "2.3.0"]
                 [cheshire "5.9.0"]
                 [camel-snake-kebab "0.4.0"]]
  :repl-options {:init-ns core-async-demo.core})
