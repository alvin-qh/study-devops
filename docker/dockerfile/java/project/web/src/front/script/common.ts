import "../css/common.less";

import Vue from "vue";
import axios, {AxiosStatic} from "axios";

declare module 'vue/types/vue' {
    interface Vue {
        $http: AxiosStatic
    }
}

Vue.prototype.$http = axios;

export default {}
