<template>
    <div class="main">
        <nav aria-label="breadcrumb">
            <ol class="breadcrumb">
                <li class="breadcrumb-item">
                    <router-link to="/">Home</router-link>
                </li>
                <li class="breadcrumb-item active" aria-current="page">Feedback</li>
            </ol>
        </nav>
        <section class="container">
            <div>
                <div class="float-left">
                    <h3>Feedback</h3>
                </div>
                <div class="float-right">
                    <button class="btn btn-primary" @click="showDialog = true">Add Feedback</button>
                </div>
            </div>
            <table class="table table-striped">
                <thead>
                <tr>
                    <th class="col-title">Title</th>
                    <th>Content</th>
                    <th class="col-created-at">Created At</th>
                    <th class="col-opt">&nbsp;</th>
                </tr>
                </thead>
                <tbody>
                <tr v-for="f in feedback">
                    <td>{{f.title}}</td>
                    <td>{{f.content}}</td>
                    <td>{{f.createdAt.format()}}</td>
                    <td>
                        <a href="javascript:;" @click="onDeleteClick(f.id)">
                            <i class="fas fa-trash-alt"></i>
                        </a>
                    </td>
                </tr>
                </tbody>
            </table>
        </section>
        <modal v-model="showDialog" title="Add Feedback" @confirm="onModalConfirm" @cancel="onModalCancel">
            <form class="form" ref="form">
                <div class="form-group">
                    <label for="title">Title</label>
                    <input type="text" class="form-control" id="title" aria-describedby="title-help"
                           placeholder="Enter title" v-model="title">
                    <small id="title-help" class="form-text text-muted">Please input feedback title</small>
                </div>
                <div class="form-group">
                    <label for="content">Content</label>
                    <textarea class="form-control" id="content" placeholder="Enter content"
                              v-model="content"></textarea>
                </div>
            </form>
        </modal>
    </div>
</template>

<script lang="ts">
    import "../common";

    import Vue from "vue";
    import Modal from "./modal.vue";
    import moment from "moment";

    interface PayloadType {
        id: number,
        title: string,
        content: string,
        createdAt: string,
        updatedAt: string
    }

    export default Vue.extend({
        data() {
            return {
                feedback: [],
                showDialog: false,
                title: '',
                content: ''
            }
        },
        components: {
            Modal
        },
        mounted() {
            this.loadData();
        },
        methods: {
            loadData() {
                this.$http.get(`/api/feedback`)
                    .then(resp => {
                        this.feedback = resp.data.payload.map((it: PayloadType) => {
                            return {
                                id: it.id,
                                title: it.title,
                                content: it.content,
                                createdAt: moment(it.createdAt),
                                updatedAt: moment(it.updatedAt)
                            };
                        });
                    })
                    .catch(error => {
                        alert('Fetch feedback list failed, try again later');
                    });
            },
            clearForm() {
                this.showDialog = false;
                this.title = '';
                this.content = '';
            },
            onModalConfirm() {
                this.$http.post(`/api/feedback`, {
                    title: this.title,
                    content: this.content
                })
                    .then(() => this.loadData())
                    .catch(error => {
                        if (error.response.status === 400) {
                            alert('Commit feedback failed, invalid input');
                        } else {
                            alert('Commit feedback failed, try again later');
                        }
                    });
                this.clearForm();
            },
            onModalCancel() {
                this.clearForm();
            },
            onDeleteClick(id: number) {
                this.$http.delete(`/api/feedback/${id}`)
                    .then(() => this.loadData());
            }
        }
    });
</script>

<style lang="less" scoped>
    .table {
        .col-title {
            width: 300px;
        }

        .col-opt {
            width: 60px;
        }

        .col-created-at {
            width: 300px
        }
    }
</style>
