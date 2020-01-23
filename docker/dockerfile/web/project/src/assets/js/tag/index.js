import "../../css/tag/index.less";

import Vue from "vue";
import {runWith} from "../common/common";

import alter from "../widget/alert";
import NameSelect from "../widget/name-select.vue";

import "../widget/breadcrumb";
import "../widget/panel";
import "../widget/panel-group";
import "../widget/notifications";
import VueNotifications from "../widget/notifications";

runWith('tag.index', function () {
    new Vue({
        el: '#app',
        components: {
            alert: alter,
            "name-select": NameSelect
        },
        data: {
            userType: ''
        },
        methods: {
            notify(text) {
                $.notify({
                    message: text
                }, {
                    type: 'info',
                    delay: 1000
                });
            },
            alertClose(type) {
                this.showAlertCloseNotify({title: `Alert ${type}`, message: `Alert ${type}`});
            }
        },
        watch: {
            userType() {
                this.showSelectedResult({message: `${this.userType} was selected`});
            }
        },
        notifications: {
            showAlertCloseNotify: {
                type: VueNotifications.types.warn,
                timeout: 500
            },
            showSelectedResult: {
                type: VueNotifications.types.success,
                timeout: 500
            }
        }
    });
});