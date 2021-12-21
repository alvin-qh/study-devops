import webpack from "webpack";

import path from "path";

import ExtractTextPlugin from "extract-text-webpack-plugin";
import HtmlPlugin from "html-webpack-plugin";
import CleanupPlugin from "webpack-cleanup-plugin";
import OptimizeCssAssetsPlugin from "optimize-css-assets-webpack-plugin";
import VueLoaderPlugin from "vue-loader/lib/plugin";

const IS_PROD = process.env.NODE_ENV === 'production';

const extractCss = new ExtractTextPlugin({
	filename: IS_PROD ? 'static/css/[name]-[chunkhash:8].css' : 'static/css/[name].css',
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
		new CleanupPlugin(),
		new HtmlPlugin({
			template: './src/template/template.html'
		})
	];

	if (IS_PROD) {
		plugins = plugins.concat([
			new OptimizeCssAssetsPlugin({
				assetNameRegExp: /\.css$/,
				cssProcessor: require('cssnano'),
				cssProcessorOptions: { discardComments: { removeAll: true } },
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
	mode: IS_PROD ? 'production' : 'development',
	entry: {
		vendor: ['vue', 'common'],
		index: ['./src/script/index.ts']
	},
	output: {
		path: path.resolve('./dist'),
		filename: IS_PROD ? 'static/script/[name]-[chunkhash:8].js' : 'static/script/[name].js',
		publicPath: "/",
		chunkFilename: IS_PROD ? 'static/script/[name]-[chunkhash:8].js' : 'static/script/[name].js',
	},
	resolve: {
		alias: {
			common: './src/script/common.ts',
			vue: IS_PROD ? 'vue/dist/vue.min.js' : 'vue/dist/vue.js'
		},
		extensions: ['.js', '.ts', '.vue', '.json']
	},
	optimization: {
		minimize: IS_PROD,
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
			test: /\.ts$/,
			exclude: [/node_modules/],
			use: [{
				loader: 'ts-loader',
				options: {
					appendTsSuffixTo: [/\.vue$/]
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
					options: { importLoaders: 1 }
				}],
				fallback: 'style-loader'
			})
		}, {
			test: /\.(eot|woff|woff2|ttf)$/,
			use: [{
				loader: 'file-loader',
				options: {
					limit: 10240,
					name: IS_PROD ? 'static/fonts/[name]-[hash:8].[ext]' : 'static/fonts/[name].[ext]',
					publicPath: '/'
				}
			}]
		}, {
			test: /\.(svg|png|jpg|gif)$/,
			use: [{
				loader: 'file-loader',
				options: {
					limit: 10240,
					name: IS_PROD ? 'static/images/[name]-[hash:8].[ext]' : 'static/images/[name].[ext]',
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
