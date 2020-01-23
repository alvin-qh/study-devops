<template>
    <div class="page-layouts">
        <header>
            <breadcrumb :previous="[
                {name:'Home', href:'/'},
                {name:'Router', href:'/router/'}
                ]">
            </breadcrumb>
        </header>

        <main class="container">
            <ul class="nav nav-tabs" role="tablist">
                <li class="nav-item" role="presentation" v-for="(router, href) in routers">
                    <a :class="linkStyle(href)" :href="href" :title="router.title">{{router.title}}</a>
                </li>
            </ul>
            <div class="card no-top-border">
                <div class="card-body">
                    <slot></slot>
                </div>
            </div>
        </main>
    </div>
</template>

<script>
    import "../../widget/breadcrumb";
    import routers from "./routers";

    export default {
        data() {
            return {
                routers: routers
            };
        },
        computed: {
            currentLink() {
                return window.location.pathname;
            }
        },
        methods: {
            linkStyle(link) {
                return this.currentLink === link ? ['nav-link', 'active'] : ['nav-link'];
            }
        }
    }
</script>