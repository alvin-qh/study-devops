<template>
    <div>
        <router-view></router-view>
    </div>
</template>

<script lang="ts">
    import VueRouter from "vue-router";
    import Index from "./index";
    import Login from "./login";

    export default {
        router: new VueRouter({
            routes: [
                {path: '/', component: Index},
                {path: '/login', component: Login}
            ]
        })
    };
</script>

<style lang="less">

</style>
