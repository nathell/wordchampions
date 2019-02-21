(defproject gridlock "0.1.0"
  :description "A starting point for re-frame based projects."
  :dependencies [[org.clojure/clojure "1.10.0"]
                 [org.clojure/clojurescript "1.10.439"]
                 [reagent "0.8.1"]
                 [re-frame "0.10.6" :exclusions [reagent]]]
  :plugins [[lein-cljsbuild "1.1.7"]
            [lein-figwheel "0.5.18"]
            [lein-sass "0.4.0"]
            [lein-resource "16.9.1"]]
  :clean-targets ^{:protect false}
    [:target-path
     [:cljsbuild :builds :app :compiler :output-dir]
     [:cljsbuild :builds :app :compiler :output-to]]
  :source-paths ["src/clj"]
  :resource-paths ["resources" "target/cljsbuild" "target/resources"]
  :figwheel {:css-dirs ["target/resources/public/css"]}
  :sass {:src              "src/sass"
         :output-directory "target/resources/public/css"}
  :resource {:resource-paths ["resources"]
             :target-path "target/resources"}
  :prep-tasks ["resource" ["sass" "once"]]
  :cljsbuild {:builds {:app {:source-paths ["src/cljs"]
                             :compiler {:output-to "target/resources/public/js/app.js"
                                        :output-dir "target/resources/public/js/out"
                                        :main "gridlock.core"
                                        :install-deps true
                                        :asset-path "js/out"
                                        :optimizations :none}}}}
  :profiles {:dev {:dependencies [[figwheel-sidecar "0.5.18"]]
                   :cljsbuild {:builds {:app {:figwheel {:on-jsload "gridlock.core/init"}}}}}
             :uberjar {:cljsbuild {:jar true
                                   :builds {:app
                                            {:compiler
                                             {:optimizations :advanced}}}}}})
