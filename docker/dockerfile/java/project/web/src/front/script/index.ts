import Vue from "vue";
import Index from "./widget/index.vue";
import { CreateElement, VNode } from "vue/types/umd";

new Vue({
    el: '#app',
    render(h: CreateElement): VNode {
        return h(Index)
    }
});
