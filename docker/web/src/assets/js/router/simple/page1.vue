<template>
    <layout>
        <div>
            Hello, This is page 1
        </div>
    </layout>
</template>

<script>
    import Layout from "./layout.vue";

    const TITLE = 'Page 1';

    export default {
        title: TITLE,
        components: {
            Layout
        },
        created() {
            document.title = TITLE;
        }
    };
</script>