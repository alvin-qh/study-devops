import "../../css/directive/index.less";

import Vue from "vue";
import {runWith} from "../common/common";
import VueNotifications from "../widget/notifications";

import "../widget/breadcrumb";

runWith('directive.index', function () {
    new Vue({
        el: '#breadcrumb'
    });

    new Vue({
        el: '#p1',
        data: {
            titleIndex: 0,
            name: '',
            gender: 'M'
        },
        created() {
            setInterval(() => this.titleIndex = this.titleIndex > 1 ? 0 : this.titleIndex + 1, 5000);
        }
    });

    new Vue({
        el: '#p2',
        data: {
            pageCount: 1,
            inputItem: '',
            selectedItem: '',
            items: []
        },
        methods: {
            gotoPage(n) {
                Pagination.showPage({title: 'Page changed', message: `Current page is ${n}`});
            },
            addNewName() {
                if (this.inputItem) {
                    this.items.push(this.inputItem);
                    if (this.items.length === 1) {
                        this.selectedItem = this.inputItem;
                    }
                    this.items.sort();
                }
            },
            selectItem(item) {
                this.selectedItem = item;
            },
            active(current) {
                return current === this.selectedItem ? 'active' : '';
            },
            deleteItem(item) {
                const idx = this.items.indexOf(item);
                if (idx >= 0) {
                    this.items = this.items.filter((item, n) => n !== idx);
                }
            },
            reverseItems() {
                this.items = this.items.reverse();
            }
        },
        computed: {
            groupedItems: {
                get() {
                    const result = {};
                    for (let item of this.items) {
                        result[item] = result[item] || 0;
                        result[item]++;
                    }
                    return result;
                }
            }
        },
        notifications: {
            showPage: {
                type: VueNotifications.types.success,
                timeout: 1000
            }
        }
    });

    new Vue({
        el: '#p3',
        data: {
            tab: 'teacher',
            subjects: ['Math', 'English', 'Chinese', 'History', 'Chemistry', 'Biology'],
            grades: [{
                name: 'First',
                classes: [1, 2, 3, 4, 5]
            }, {
                name: 'Second',
                classes: [1, 2, 3, 4]
            }, {
                name: 'Third',
                classes: [1, 2, 3, 4]
            }, {
                name: 'Fourth',
                classes: [1, 2, 3]
            }],
            student: {
                name: '',
                gender: 'M',
                phone: '',
                grade: '',
                class: '',
                remark: ''
            },
            teacher: {
                name: '',
                gender: 'M',
                subject: '',
                phone: '',
                remark: ''
            },
            other: {
                name: '',
                phone: ''
            }
        },
        computed: {
            gradeClass: {
                get() {
                    if (this.student.grade) {
                        const grade = this.grades.find((g) => g.name === this.student.grade);
                        if (grade) {
                            return grade.classes;
                        }
                    }
                    return [];
                }
            }
        },
        watch: {
            'student.grade': () => this.student.class = ''
        },
        methods: {
            active(name) {
                return name === this.tab ? 'active' : '';
            },
            changeTab(name) {
                this.tab = name;
            }
        }
    });
});
