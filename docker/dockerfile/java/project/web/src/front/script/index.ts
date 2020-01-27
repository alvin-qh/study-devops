import Vue from "vue";
import VueRouter from "vue-router";
import Index from "./widget/index.vue";
import Feedback from "./widget/feedback.vue";
// import {CreateElement, VNode} from "vue/types/umd";
Vue.use(VueRouter);


const router = new VueRouter({
    routes: [
        {path: '/', component: Index},
        {path: '/feedback', component: Feedback}
    ]
});

new Vue({
    el: '#app',
    router: router,
    // render(h: CreateElement): VNode {
    //     return h(Index)
    // }
});
