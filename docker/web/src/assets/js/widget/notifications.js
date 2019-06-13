/*
 * https://se-panfilov.github.io/vue-notifications
 */
import "noty/lib/noty.css";
import "noty/lib/themes/bootstrap-v4.css";

import Vue from "vue";
import VueNotifications from 'vue-notifications';
import Noty from 'noty';

function toast({title, message, type, timeout, cb}) {
    if (type === VueNotifications.types.warn) {
        type = 'warning';
    }
    return new Noty({text: message, timeout, type, theme: 'bootstrap-v4'}).show();
}

Vue.use(VueNotifications, {
    success: toast,
    error: toast,
    info: toast,
    warn: toast
});

export default VueNotifications;