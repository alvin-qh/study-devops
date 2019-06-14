<template>
    <el-menu :default-active="activeIndex" class="el-menu-demo" mode="horizontal" @select="handleSelect">
        <el-submenu index="1">
            <template slot="title">{{ $t("menu.about") }}</template>
            <el-menu-item index="1-1">{{ $t("menu.about_getting_started") }}</el-menu-item>
            <el-submenu index="1-3">
                <template slot="title">{{ $t("menu.about_demo") }}</template>
            </el-submenu>
        </el-submenu>
        <el-menu-item index="2">{{ $t("menu.document") }}</el-menu-item>
        <el-submenu index="3">
            <template slot="title">{{ $t("menu.workspace") }}</template>
        </el-submenu>
        <el-submenu index="5" class="menu-last">
            <template slot="title">{{ $t("menu.current_user") }}</template>
            <el-menu-item index="5-1">{{ $t("menu.my_logout") }}</el-menu-item>
        </el-submenu>
    </el-menu>
</template>

<script lang="js">
    export default {
        data() {
            return {
                activeIndex: "1"
            }
        },
        methods: {
            handleSelect(index) {
            }
        }
    };
</script>

<style lang="less">
    .menu-last {
        float: right !important;
    }
</style>
