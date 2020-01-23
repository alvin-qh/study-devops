import "../../css/widget/panel.less";

import Vue from "vue";

const template = `<div class="widget-panel">
    <div class="card">
        <div v-if="title" :class="titleStyle">
            <a v-if="isCollapsible" href="javascript:;" role="button" data-toggle="collapse" aria-expanded="true" @click="collapseMe">{{ title }}</a>
            <template v-else>{{ title }}</template>
        </div>
        <div class="card-body">
            <slot></slot>
        </div>
        <div v-if="footer" class="card-footer">{{ footer }}</div>
    </div>
</div>`;

Vue.component('panel', {
    template: template,
    data() {
        return {
            timer: null,
            headerHeight: 0,
            bodyHeight: 0
        };
    },
    props: {
        type: [String],
        title: [String],
        footer: [String],
        collapsible: {
            type: [Boolean, String],
            default: true
        },
        speed: {
            type: String,
            default: 'normal'
        },
        collapsed: {
            type: [String, Boolean],
            default: false
        }
    },
    created() {
        if (this.$parent && this.$parent.add) {
            this.$parent.add(this);
        }
    },
    mounted() {
        this.headerHeight = this.$el.querySelector('div.card-header').offsetHeight + 1;
        this.bodyHeight = this.$el.querySelector('div.card-body').offsetHeight + 1;
        const $footer = this.$el.querySelector('div.card-footer');
        if ($footer) {
            this.bodyHeight += $footer.offsetHeight;
        }
    },
    computed: {
        isCollapsible() {
            return this.collapsible === true || this.collapsible === 'true';
        },
        titleStyle() {
            return ['card-header', `bg-${this.type}`];
        },
        animateSpeed() {
            let interval = 20;
            switch (this.speed) {
                case 'fast':
                    interval = 10;
                    break;
                case 'slow':
                    interval = 40;
                    break;
            }
            return interval;
        }
    },
    methods: {
        isCollapsed() {
            return this.$el.offsetHeight > this.headerHeight;
        },
        collapseMe() {
            if (this.timer == null) {
                if (this.isCollapsed()) {
                    this.fold();
                } else {
                    this.unfold();
                }
            }
        },
        stop() {
            if (this.timer != null) {
                clearInterval(this.timer);
                this.timer = null;
            }
        },
        fold() {
            this.stop();
            let height = this.$el.offsetHeight;

            this.timer = setInterval(() => {
                if (height <= this.headerHeight) {
                    this.stop();
                    height = this.headerHeight;
                } else {
                    height -= Math.min(10, height - this.headerHeight);
                }
                this.$el.style.height = `${height}px`;
            }, this.animateSpeed);
        },
        unfold() {
            this.stop();
            if (this.$parent && this.$parent.expanding) {
                this.$parent.expanding(this);
            }

            let height = this.$el.offsetHeight;

            this.timer = setInterval(() => {
                const fullHeight = this.headerHeight + this.bodyHeight;
                if (height >= fullHeight) {
                    this.stop();
                    height = fullHeight;
                } else {
                    height += Math.min(10, fullHeight - height);
                }
                this.$el.style.height = `${height}px`;
            }, this.animateSpeed);
        }
    },
    watch: {
        collapsed(val) {
            if (val) {
                this.fold();
            } else {
                this.unfold();
            }
        }
    }
});