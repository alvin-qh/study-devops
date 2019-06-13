<template>
    <div>
        <header>
            <breadcrumb :previous="[{name:'Home', href:'/'}, {name:'Vuex', href:'/vuex'}]"></breadcrumb>
        </header>

        <main class="container">
            <div class="row form-group">
                <label class="col-1 col-form-label text-right">Operator: </label>
                <div class="col-3 input-group">
                    <div class="input-group-prepend">
                        <button class="btn btn-primary" @click="decrement()">-</button>
                    </div>

                    <input type="text" class="form-control" readonly :value="count"/>

                    <div class="input-group-append">
                        <button class="btn btn-primary" @click="increment()">+</button>
                    </div>
                </div>
            </div>
        </main>
    </div>
</template>

<script lang="ts">
    import Vue from "vue";
    import {basicStore} from "./stores";

    import "../../widget/breadcrumb";

    export default Vue.extend({
        data() {
            return {
            };
        },
        created() {
            document.title = this.$router.currentRoute.name as string;
        },
        computed: {
            count() {
                return basicStore.state.count;
            }
        },
        methods: {
            decrement() {
                basicStore.commit('decrement')
            },
            increment() {
                basicStore.commit('increment')
            }
        }
    })
</script>


<style lang="less" scoped>
</style>