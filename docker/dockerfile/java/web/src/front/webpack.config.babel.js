import webpack from "webpack";

import glob from "glob";
import path from "path";

import ExtractTextPlugin from "extract-text-webpack-plugin";
import HtmlPlugin from "html-webpack-plugin";
import OptimizeCssAssetsPlugin from "optimize-css-assets-webpack-plugin";
import VueLoaderPlugin from "vue-loader/lib/plugin";
import CleanWebpackPlugin from "clean-webpack-plugin";

const CONFIG = {
    isProd: (process.env.NODE_ENV === 'production'),
    paths: {
        scripts: file => path.join(__dirname, 'scripts', file || ''),
        styles: file => path.join(__dirname, 'styles', file || ''),
        temps: file => path.join(__dirname, 'templates', file || '')
    }
};

function makeEntries() {
    const src = CONFIG.paths.scripts();
    const entries = {};

    glob.sync(path.join(src, '/**/main.js'))
        .forEach(file => {
            let name = path.dirname(file);
            name = name.substr(name.lastIndexOf('/') + 1);
            entries[name] = file;
        });
    return entries;
}

function makeTemplates() {
    return glob.sync(path.join(CONFIG.paths.temps(), '/**/*.html'))
        .map(file => {
            let filename = file.replace(CONFIG.paths.temps() + '/', '');
            let chunks = ['manifest', 'vendor', 'common', filename.substr(0, filename.indexOf('/')) || 'home'];

            return new HtmlPlugin({
                filename: path.join(__dirname, '../main/resources/templates/', filename),
                template: file,
                inject: true,
                chunks: chunks,
                cache: true,
                chunksSortMode(a, b) {
                    return chunks.indexOf(a) - chunks.indexOf(b);
                },
                minify: {
                    collapseWhitespace: CONFIG.isProd,
                    removeComments: CONFIG.isProd
                }
            });
        });
}

const extractCss = new ExtractTextPlugin({
    filename: 'static/css/[name]-[chunkhash:8].css',
    disable: false,
    allChunks: true,
});

const plugins = (() => {
    const ProvidePlugin = webpack.ProvidePlugin;
    const HotModuleReplacementPlugin = webpack.HotModuleReplacementPlugin;

    let plugins = [
        new VueLoaderPlugin(),
        new ProvidePlugin({}),
        extractCss,
        new CleanWebpackPlugin(['templates', 'static/css', 'static/js'], {
            root: path.join(__dirname, '../main/resources'),
            exclude: [],
            verbose: true,
            dry: false
        })
    ].concat(makeTemplates());

    if (CONFIG.isProd) {
        plugins = plugins.concat([
            new OptimizeCssAssetsPlugin({
                assetNameRegExp: /\.css$/,
                cssProcessor: require('cssnano'),
                cssProcessorOptions: {discardComments: {removeAll: true}},
                canPrint: true
            })
        ]);
    } else {
        plugins = plugins.concat([
            new HotModuleReplacementPlugin()
        ]);
    }
    return plugins;
})();

export default {
    mode: CONFIG.isProd ? 'production' : 'development',
    entry: Object.assign({vendor: ['vue', 'axios', 'moment', 'lodash', 'common']}, makeEntries()),
    output: {
        path: path.resolve(path.join(__dirname, '../main/resources/')),
        filename: 'static/js/[name]-[hash:8].js',
        publicPath: "/",
        chunkFilename: 'static/js/[name]-[chunkhash:8].js',
        hotUpdateChunkFilename: '../../../build/webpack/hot-update.js',
        hotUpdateMainFilename: '../../../build/webpack/hot-update.json'
    },
    resolve: {
        alias: {
            common: CONFIG.paths.scripts('/common/common.js'),
            vue: CONFIG.isProd ? 'vue/dist/vue.min.js' : 'vue/dist/vue.js'
        },
        extensions: ['.js', '.vue', '.json']
    },
    optimization: {
        minimize: CONFIG.isProd,
        removeEmptyChunks: true,
        splitChunks: {
            chunks: 'all',
            name: 'vendor'
        },
        runtimeChunk: {
            name: 'manifest',
        }
    },
    module: {
        rules: [{
            test: /\.js$/,
            exclude: [/node_modules/],
            use: [{
                loader: 'babel-loader',
                options: {
                    presets: ['@babel/env']
                }
            }]
        }, {
            test: /\.css/,
            use: extractCss.extract({
                use: [{
                    loader: 'css-loader'
                }],
                fallback: 'style-loader'
            })
        }, {
            test: /\.less$/,
            use: extractCss.extract({
                use: [{
                    loader: 'css-loader',
                }, {
                    loader: 'less-loader',
                    options: {importLoaders: 1}
                }],
                fallback: 'style-loader'
            })
        }, {
            test: /\.(eot|woff|woff2|ttf)$/,
            use: [{
                loader: 'file-loader',
                options: {
                    limit: 10240,
                    name: 'static/fonts/[name]-[hash:8].[ext]',
                    publicPath: '/'
                }
            }]
        }, {
            test: /\.(svg|png|jpg|gif)$/,
            use: [{
                loader: 'file-loader',
                options: {
                    limit: 10240,
                    name: 'static/images/[name]-[hash:8].[ext]',
                    publicPath: '/'
                }
            }]
        }, {
            test: /\.vue$/,
            exclude: [/node_modules/],
            loader: 'vue-loader'
        }]
    },
    plugins: plugins,
    devServer: {
        contentBase: path.resolve('out/'),
        inline: true,
        hot: true
    },
    devtool: 'cheap-source-map',
};
