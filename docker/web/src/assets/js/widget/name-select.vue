<template>
    <div class="form-group row">
        <label :for="id" class="col-form-label text-right">{{label}}: </label>
        <select :id="id" :name="name" class="form-control" v-model="selected">
            <slot></slot>
        </select>
    </div>
</template>

<script>
    export default {
        props: {
            label: String,
            name: String,
            value: ''
        },
        data() {
            return {
                selected: ''
            }
        },
        computed: {
            id() {
                return `form-control-${this.name}`;
            }
        },
        watch: {
            selected() {
                this.$emit('input', this.selected);
            }
        }
    }
</script>

<style scoped lang="less">
    .form-group {
        select {
            width: 220px;
            margin-left: 10px;
        }
    }
</style>