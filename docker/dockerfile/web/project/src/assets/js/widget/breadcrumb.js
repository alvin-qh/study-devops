import Vue from "vue";
import * as _ from "lodash";

const template = `<nav aria-label="breadcrumb"> 
    <ol class="breadcrumb"> 
        <li v-for="item in previous" class="breadcrumb-item">
            <a :href="item.href">{{item.name}}</a>
        </li> 
        <li class="breadcrumb-item active">{{title}}</li> 
    </ol> 
</nav>`;

Vue.component('breadcrumb', {
    template: template,
    data() {
        return {
            title: document.title
        };
    },
    props: {
        previous: {
            type: Array,
            require: true,
            validator(vals) {
                if (!_.isArray(vals)) {
                    return false;
                }

                let ok = true;
                for (let val of vals) {
                    if (typeof val !== 'object') {
                        ok = false;
                    } else {
                        ok = val.name && val.href;
                    }
                    if (!ok) {
                        return false;
                    }
                }
                return ok;
            },
            default() {
                return [];
            }
        }
    }
});