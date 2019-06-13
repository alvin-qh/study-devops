import "../../css/router/index.less";

import Vue from "vue";
import { runWith } from "../common/common";

import "../widget/breadcrumb";

runWith('router.index', function() {
    new Vue({
        el: '#app',
        data: {
            links: [
                { name: 'Simple', href: '/router/simple.html' },
                { name: 'Page', href: '/router/page.html' },
                { name: 'Vue', href: '/router/vue.html' }
            ]
        }
    });
});