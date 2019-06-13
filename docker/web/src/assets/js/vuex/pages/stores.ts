import Vue from "vue";
import Vuex from "vuex";

Vue.use(Vuex);

export const basicStore = new Vuex.Store({
    state: {
        count: 0
    },
    mutations: {
        increment: state => state.count++,
        decrement: state => state.count > 0 ? state.count-- : state.count
    }
});

