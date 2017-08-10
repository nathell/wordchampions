console.log('__dirname is', __dirname);
var webpack = require('webpack');
module.exports = {
    entry: "./webpack-entry.js",
    output: {
        path: __dirname + "/target/cljsbuild/public/js",
        filename: "webpack-bundle.js"
    },
    module: {
        loaders: [
            { test: /\.js$/, exclude: /node_modules/, loader: "babel-loader" },
            { test: /\.css$/, loader: "style!css" }
        ]
    },
    plugins: [ new webpack.EnvironmentPlugin(['NODE_ENV']) ]
};
