<template>
    <div class="main">
        <header>
            <nav class="navbar navbar-dark bg-dark">
                <a class="navbar-brand" href="javascript:;">
                    <img :src="logo" width="30" height="30"/> Docker Web
                </a>
                <router-link class="feedback" to="/feedback">Feedback</router-link>
            </nav>
        </header>
        <section class="container">
            <div class="card">
                <div class="card-body">
                    <h3 class="card-title">Welcome to Docker</h3>
                    <img :src="dockerImg"/>
                </div>
            </div>
        </section>
    </div>
</template>

<script lang="ts">
    import Vue from "vue";

    import logo from "./image/docker-logo.png";
    import dockerImg from "./image/docker.png";

    export default Vue.extend({
        data() {
            return {
                logo: logo,
                dockerImg: dockerImg
            };
        }
    });
</script>

<style lang="less" scoped>
    header {
        a {
            &.feedback {
                color: #ebebeb;
            }
        }
    }

    .container {
        margin-top: 10px;

        .card-body {
            .card-title {
                text-align: center;
            }

            img {
                width: 100%;
            }
        }
    }
</style>
