const path = require("path");
const webpack = require("webpack");
const MiniCssExtractPlugin = require("mini-css-extract-plugin");
const HtmlPlugin = require("html-webpack-plugin");
const VueLoaderPlugin = require("vue-loader/lib/plugin");

const IS_PROD = process.env.NODE_ENV === "production";

const makePlugins = () => {
  const HotModuleReplacementPlugin = webpack.HotModuleReplacementPlugin;

  let plugins = [
    new VueLoaderPlugin(),
    new MiniCssExtractPlugin({
      filename: "static/style/[name].bundle-[contenthash:8].css"
    }),
    new HtmlPlugin({
      filename: 'templates/index.html',
      template: './template/template.html'
    })
  ];

  if (!IS_PROD) {
    plugins = plugins.concat([
      new HotModuleReplacementPlugin()
    ]);
  }

  return plugins;
};

module.exports = {
  mode: IS_PROD ? "production" : "development",
  devtool: "cheap-source-map",
  entry: {
    vendor: ["vue", "axios", "moment", "common"],
  },
  resolve: {
    alias: {
      common: "./script/common.ts",
      vue: IS_PROD ? "vue/dist/vue.min.js" : "vue/dist/vue.js"
    },
    extensions: [".ts", ".tsx", ".js", ".css", ".jsx"]
  },
  output: {
    path: path.resolve("../main/resources"),
    filename: "static/script/[name]-[hash:8].js",
    publicPath: "/",
    chunkFilename: "static/script/[name]-[hash:8].js",
  },
  plugins: makePlugins(),
  module: {
    rules: [
      {
        test: /\.(js|es)$/i,
        exclude: [/node_modules/i],
        use: {
          loader: "babel-loader"
        }
      },
      {
        test: /\.(ts|tsx)$/i,
        exclude: /node_modules/i,
        use: {
          loader: "ts-loader"
        }
      },
      {
        test: /\.css$/i,
        use: [
          {
            loader: MiniCssExtractPlugin.loader
          },
          {
            loader: "css-loader"
          }
        ]
      },
      {
        test: /\.less$/i,
        use: [
          {
            loader: MiniCssExtractPlugin.loader
          },
          {
            loader: "css-loader"
          },
          {
            loader: "less-loader"
          }
        ]
      },
      {
        test: /\.(eot|woff|woff2|ttf)$/i,
        type: "asset/resource",
        parser: {
          dataUrlCondition: {
            maxSize: 128 * 1024
          }
        },
        generator: {
          filename: "static/fonts/[name]-[hash:8].[ext]"
        }
      },
      {
        test: /\.(svg|png|jpg|gif)$/i,
        type: "asset/resource",
        parser: {
          dataUrlCondition: {
            maxSize: 128 * 1024
          }
        },
        generator: {
          filename: "static/images/[name]-[hash:8].[ext]"
        }
      },
      {
        test: /\.vue$/i,
        exclude: /node_modules/i,
        loader: "vue-loader"
      }
    ]
  }
};
