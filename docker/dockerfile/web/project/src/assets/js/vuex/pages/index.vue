<template>
    <div>
        <header>
            <breadcrumb :previous="[{name:'Home', href:'/'}]"></breadcrumb>
        </header>
        <main class="container">
            <ul class="list-group">
                <li class="list-group-item">
                    <router-link to="/basic">Basic</router-link>
                </li>
            </ul>
        </main>
    </div>
</template>

<script lang="ts">
    import Vue from "vue";
    import "../../widget/breadcrumb";

    export default Vue.extend({})
</script>

<style lang="less" scoped>
</style>