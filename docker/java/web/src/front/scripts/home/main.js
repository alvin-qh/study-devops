import '../../styles/home/main.less';

import Vue from "vue";
import VueRouter from "vue-router";
import VueI18n from "vue-i18n";
import ElementUI from "element-ui";

import Root from "./root";
import messages from "../i18n/messages";

Vue.use(VueRouter);
Vue.use(VueI18n);
Vue.use(ElementUI);

const vue = new Vue({
    el: '#main',
    components: {
        Root
    },
    i18n: new VueI18n({
        locale: 'en',
        messages
    })
});
