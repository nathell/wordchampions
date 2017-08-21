(defproject kobrem "0.1.0"
  :description "A starting point for re-frame based projects."
  :dependencies [[org.clojure/clojure "1.9.0-alpha17"]
                 [re-frame "0.9.4"]
                 [org.clojure/clojurescript "1.9.854" :scope "provided"]
                 [reagent "0.6.0" :exclusions [cljsjs/react cljsjs/react-dom]]]
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
                                        :main "kobrem.core"
                                        :asset-path "js/out"
                                        :optimizations :none
                                        :pretty-print true}}}}
  :profiles {:dev {:cljsbuild {:builds {:app {:figwheel true}}}}
             :uberjar {:cljsbuild {:jar true
                                   :builds {:app
                                            {:compiler
                                             {:optimizations :whitespace
                                              :pretty-print false}}}}}})
