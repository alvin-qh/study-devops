import "../../css/bind/index.less";

import Vue from "vue";
import {runWith, Times} from "../common/common";

import "../widget/breadcrumb";

runWith('bind.index', function () {
    new Vue({
        el: '#app',
        data: {
            text: '',
            color: 'info',
            panelColor: 'info',
            now: Times.nowString()
        },
        computed: {
            textInverse: {
                get() {
                    return this.text.split('').reverse().join('');
                },
                set(newVal) {
                    this.text = newVal.split('').reverse().join('');
                }
            },
            noRefresh() {
                return Times.nowString();
            },
            refresh() {
                return this.now;
            }
        },
        watch: {
            color(val) {
                switch (val) {
                case 'info':
                    this.panelColor = 'info';
                    break;
                case 'warning':
                    this.panelColor = 'warning';
                    break;
                case 'danger':
                    this.panelColor = 'danger';
                    break;
                }
            }
        },
        created() {
            setInterval(() => this.now = Times.nowString(), 1000);
        }
    });
});