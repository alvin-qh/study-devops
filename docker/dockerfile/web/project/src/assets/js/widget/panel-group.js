'use strict';

import Vue from "vue";

const template = `<div class="card-deck" role="tablist" aria-multiselectable="true">
    <slot></slot>
</div>`;

Vue.component('panel-group', {
    template: template,
    data() {
        return {
            panels: new Set()
        };
    },
    methods: {
        add(panel) {
            this.panels.add(panel);
        },
        expanding(panel) {
            this.panels.forEach(p => {
                if (p !== panel) {
                    p.fold();
                }
            });
        }
    }
});