<template>
    <a :href="href" @click="go">
        <slot></slot>
    </a>
</template>

<script>
    export default {
        props: {
            href: {
                type: String,
                required: true
            },
            routers: {
                type: Object,
                required: true
            }
        },
        methods: {
            go (event) {
                event.preventDefault();
                window.history.pushState(null, this.routers[this.href], this.href);
                this.$emit('input', this.href);
            }
        }
    }
</script>