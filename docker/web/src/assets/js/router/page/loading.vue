<template>
    <div class="container-fluid">
        <div class="loading">Loading...</div>
    </div>
</template>

<script>
    import Layout from "./layout.vue";

	export default {
		title: 'Loading',
		components: {
			Layout
		},
		created() {
			document.title = comp.title;
		}
	};
</script>

<style lang="less" scoped>
    .loading {
        text-align: center;
        margin-top: 30px;
    }
</style>