import "../../css/home/index.less";

import Vue from "vue";
import {Navbar} from 'bootstrap-vue/es/components';

import {runWith, Times} from "../common/common";

runWith('home.index', function () {
    Vue.use(Navbar);

    new Vue({
        el: '#app',
        data: {
            title: 'Vue Demos',
            datetime: Times.nowString()
        },
        created() {
            setInterval(() => this.datetime = Times.nowString(), 1000);
        }
    });
});