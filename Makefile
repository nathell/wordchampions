OUT := resources/public

all: $(OUT)/css/app.css watch
release: $(OUT)/css/app.css $(OUT)/js/app.js

watch:
	shadow-cljs watch app

$(OUT)/css/app.css: src/sass/app.sass
	mkdir -p $(OUT)/css
	sassc $< $@

$(OUT)/js/app.js: src/cljs/gridlock/*.cljs
	shadow-cljs release app

clean:
	rm -rf $(OUT)/js $(OUT)/css

.PHONY: all clean release watch
