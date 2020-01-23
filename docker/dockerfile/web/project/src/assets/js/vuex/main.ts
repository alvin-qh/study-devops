import "../../css/vuex/index.less";

import Vue from "vue";
import VueRouter from "vue-router";
import IndexPage from "./pages/index";
import BasicPage from "./pages/basic";

import {runWith} from "../common/common";

runWith('vuex.index', function () {
    Vue.use(VueRouter);

    new Vue({
        el: '#app',
        router: new VueRouter({
            routes: [{
                path: '/index',
                component: IndexPage,
                name: 'Index'
            }, {
                path: '/basic',
                component: BasicPage,
                name: 'Basic'
            }],
            mode: 'hash'
        }),
        created() {
            if (this.$router.currentRoute.matched.length === 0) {
                this.$router.replace('/index');
            }
        }
    });
});