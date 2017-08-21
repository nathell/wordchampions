(defproject gridlock "0.1.0"
  :description "A starting point for re-frame based projects."
  :dependencies [[org.clojure/clojure "1.9.0-alpha17"]
                 [cljsjs/material-ui "0.18.7-0"]
                 [re-frame "0.10.1" :exclusions [cljsjs/react cljsjs/react-dom]]
                 [org.clojure/clojurescript "1.9.917" :scope "provided"]]
  :plugins [[lein-cljsbuild "1.1.7"]
            [lein-figwheel "0.5.12"]]
  :clean-targets ^{:protect false}
    [:target-path
     [:cljsbuild :builds :app :compiler :output-dir]
     [:cljsbuild :builds :app :compiler :output-to]]
  :source-paths ["src/clj"]
  :resource-paths ["resources" "target/cljsbuild" "target/resources"]
  :cljsbuild {:builds {:app {:source-paths ["src/cljs"]
                             :compiler {:output-to "target/cljsbuild/public/js/app.js"
                                        :output-dir "target/cljsbuild/public/js/out"
                                        :main "gridlock.core"
                                        :npm-deps {:react "15.6.1"
                                                   :react-dnd "2.4.0"
                                                   :react-dnd-html5-backend "2.4.1"}
                                        :install-deps true
                                        :asset-path "js/out"
                                        :optimizations :none
                                        :infer-externs true
                                        :pretty-print true}}}}
  :profiles {:dev {:cljsbuild {:builds {:app {:figwheel true}}}}
             :uberjar {:cljsbuild {:jar true
                                   :builds {:app
                                            {:compiler
                                             {:optimizations :advanced
                                              :pretty-print false}}}}}})
