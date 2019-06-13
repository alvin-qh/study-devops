import "../../css/router/router.less";

import Vue from "vue";
import * as _ from "lodash";
import {runWith} from "../common/common";

import routers from "./simple/routers";
import Page404 from "./simple/404.vue";

runWith('router.simple', function () {
    _.each(routers, (router, href) => {
        window.history.pushState(null, router.title, href);
        return false;
    });

    new Vue({
        el: '#app',
        data: {
            currentLink: window.location.pathname
        },
        computed: {
            currentView() {
                const matchingView = routers[this.currentLink];
                return matchingView || Page404;
            }
        },
        created() {
            window.addEventListener('popstate', () => {
                this.currentLink = window.location.pathname;
            });
        },
        render(h) {
            return h(this.currentView)
        }
    });
});