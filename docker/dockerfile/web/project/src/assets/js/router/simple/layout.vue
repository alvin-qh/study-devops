<template>
    <div class="simple-layout">
        <header>
            <breadcrumb :previous="[
                {name:'Home', href:'/'},
                {name:'Router', href:'/router/'}]">
            </breadcrumb>
        </header>

        <main class="container">
            <ul class="nav nav-tabs" role="tablist">
                <li v-for="(router, href) in routers" class="nav-item" role="presentation">
                    <v-link :class="linkStyle(href)" :href="href" :title="router.title" :routers="routers"
                            v-model="currentLink">
                        {{ router.title }}
                    </v-link>
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
    import VLink from "./v-link.vue";
    import routers from "./routers";
    import "../../widget/breadcrumb";

    export default {
        components: {
            VLink
        },
        data() {
            return {
                routers: routers,
                currentLink: window.location.pathname
            };
        },
        methods: {
            linkStyle(link) {
                return this.currentLink === link ? ['nav-link', 'active'] : ['nav-link'];
            }
        },
        watch: {
            currentLink() {
                this.$root.currentLink = this.currentLink;
            }
        }
    }
</script>