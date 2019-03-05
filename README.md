# gridlock

A fun word game!

## Building and hacking

To launch a dev environment with Emacs and Figwheel:

 1. Have Leiningen, Emacs, sassc and a recent CIDER installed
 2. `M-x cider-jack-in-clojurescript`
 3. The app should pop on http://localhost:3449

Outside of Emacs, `lein figwheel` should work.

To build a production version: `lein with-profile uberjar cljsbuild once`. This will produce a deployable version in `target/resources/public/`.

## Play it

http://danieljanus.pl/wladcyslow/

It’s in Polish only, for now.
