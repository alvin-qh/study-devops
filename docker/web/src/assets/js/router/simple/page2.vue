<template>
    <layout>
        <div>
            Hello, This is page 2
        </div>
    </layout>
</template>

<script>
    import Layout from "./layout.vue";

    const TITLE = 'Page 2';

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