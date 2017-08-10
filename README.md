# kobrem

A bare minimal starting point for ClojureScript projects based on
re-frame. Doesn’t assume a Clojure backend.

## Building it

1. `lein cljsbuild auto`
2. Copy `index.html` to `target/cljsbuild/public/`
3. `cd target/cljsbuild/public/; python -mSimpleHTTPServer`
4. Navigate to http://localhost:8000
