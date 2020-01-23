<template>
    <el-container>
        <el-header>
            <Menu></Menu>
        </el-header>
        <el-main>
            <el-card class="box-card">
                <div slot="header" class="clearfix">
                    <span>{{ $t("about.about") }}</span>
                </div>
                <div class="text item">
                    {{ $t("about.version") }}: {{ about.version }}
                </div>
                <div class="text item">
                    {{ $t("about.zone") }}: {{ about.zone }}
                </div>
            </el-card>
        </el-main>
    </el-container>
</template>

<script lang="ts">
    import Menu from "./comps/menu";
    import axios from "axios";

    export default {
        components: {Menu},
        data() {
            return {
                about: {
                    version: '',
                    zone: ''
                }
            }
        },
        mounted() {
            axios.get('/api/about')
                .then(resp => {
                    this.about = resp.data
                })
                .catch(error => {
                    alert(error);
                });
        }
    };
</script>

<style lang="less">
    .version {
        text-align: center;
    }
</style>
