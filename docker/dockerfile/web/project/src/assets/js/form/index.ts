import "../../css/form/index.less";

import Vue from "vue";
import {runWith} from "../common/common";
import hljs from "highlight.js"
import "../widget/breadcrumb";

import VeeValidate, {Validator} from "vee-validate";

declare interface Birthday {
    year: number,
    month: number,
    date: number
}

class Form {
    name: string = '';
    gender: string = 'M';
    birthday: Birthday = {year: 0, month: 0, date: 0};
    hobbies: Array<string> = [];
    remark: string = '';
}

runWith('form.index', function () {
    Vue.use(VeeValidate);

    new Vue({
        el: '#app',
        data: {
            form: new Form(),
            formJson: '',
            submit: false
        },
        created() {
            const now = new Date();
            const year = now.getFullYear(), month = now.getMonth() + 1, date = now.getDate();
            this.form.birthday = {year, month, date};

            hljs.initHighlightingOnLoad();

            (vue => {
                Validator.extend('hobbies', {
                    getMessage(field: any) {
                        return 'One of hobbies must be checked';
                    },
                    validate(value: any) {
                        return vue.form.hobbies && vue.form.hobbies.length > 0;
                    }
                });
            })(this);
        },
        mounted() {
            (this as any).$hljs = document.querySelector('.hljs.json') as HTMLElement;
        },
        computed: {
            now(): Date {
                return new Date();
            },
            lastMonthDay(): number {
                if (this.form.birthday.month) {
                    const now = new Date();
                    now.setMonth(this.form.birthday.month);
                    now.setDate(0);
                    return now.getDate();
                }
                return 0;
            }
        },
        watch: {
            form: {
                handler() {
                    const $hljs = (this as any).$hljs;
                    $hljs!.innerText = JSON.stringify(this.form, null, '    ');
                    hljs.highlightBlock($hljs!);
                },
                deep: true
            }
        },
        methods: {
            onSubmit() {
                this.$validator.validateAll().then(result => {
                    this.submit = result;
                });
            }
        }
    });
});