<template>
    <el-menu :default-active="activeIndex" class="el-menu-demo" mode="horizontal" @select="handleSelect">
        <el-submenu index="1">
            <template slot="title">{{ $t("menu.about") }}</template>
            <el-menu-item index="1-1">{{ $t("menu.about_getting_started") }}</el-menu-item>
            <el-menu-item index="1-2">{{ $t("menu.about_update_list") }}</el-menu-item>
            <el-submenu index="1-3">
                <template slot="title">{{ $t("menu.about_demo") }}</template>
                <el-menu-item index="1-3-1">{{ $t("menu.about_demo_taipei") }}</el-menu-item>
            </el-submenu>
        </el-submenu>
        <el-menu-item index="2">{{ $t("menu.document") }}</el-menu-item>
        <el-submenu index="3">
            <template slot="title">{{ $t("menu.workspace") }}</template>
            <el-menu-item index="3-1">{{ $t("menu.workspace_workflow") }}</el-menu-item>
            <el-menu-item index="3-2">{{ $t("menu.workspace_org") }}</el-menu-item>
            <el-menu-item index="3-3">{{ $t("menu.workspace_user") }}</el-menu-item>
        </el-submenu>
        <el-submenu index="5" class="menu-last">
            <template slot="title">Current User</template>
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
                switch (index) {
                    case "1-1":
                        location.href = '/static/doc/about/getting-started.html';
                        break;
                    case "1-2":
                        location.href = '/static/doc/about/update.html';
                        break;
                    case "1-3-1":
                        location.href = '/static/doc/about/demo-taipei-university.html';
                        break;
                    case "2":
                        location.href = '/static/doc/api/index.html';
                        break;
                    case "4":
                        this.$router.push('/login');
                        break;
                }
            }
        }
    };
</script>

<style lang="less">
    .menu-last {
        float: right !important;
    }
</style>
