<template>
    <div class="widget-modal">
        <transition name="fade">
            <div class="modal" v-show="showModal">
                <div class="modal-dialog shadow">
                    <div class="modal-content">
                        <div class="modal-header">
                            <h4>{{title}}</h4>
                            <button type="button" class="close" data-dismiss="modal" aria-label="Close"
                                    @click="showModal=false">
                                <span aria-hidden="true">&times;</span>
                            </button>
                        </div>
                        <div class="modal-body">
                            <slot></slot>
                        </div>
                        <div class="modal-footer">
                            <button type="button" class="btn btn-outline-secondary" @click="$emit('cancel')">Cancel</button>
                            <button type="button" class="btn btn-primary" @click="$emit('confirm')">Confirm</button>
                        </div>
                    </div>
                </div>
            </div>
        </transition>
    </div>
</template>

<script lang="ts">
    import Vue from "vue";

    export default Vue.extend({
        props: {
            value: {
                type: [Boolean, String],
                default: false
            },
            title: {
                type: String,
                default: ''
            }
        },
        data() {
            return {
                showModal: this.value === 'true' || this.value === true
            }
        },
        watch: {
            value() {
                this.showModal = this.value === 'true' || this.value === true;
            },
            showModal() {
                this.$emit('input', this.showModal);
            }
        }
    });
</script>

<style lang="less" scoped>
    .modal {
        display: block;
        background: rgba(0, 0, 0, 0.3);
    }

    .fade-enter-active {
        transition: opacity .5s;
    }

    .fade-enter {
        opacity: 0;
    }
</style>
