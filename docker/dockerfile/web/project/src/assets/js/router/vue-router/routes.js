import Page1 from './pages/page1.vue';
import Page2 from './pages/page2.vue';

export default [{
    path: '/page1',         // 对应 <router-link to='/page1'></router-link>
    components: {
        'default': Page1    // 对应 <router-view name='default'></router-view>
    },
    name: 'Page1'
}, {
    path: '/page2',
    components: {
        'default': Page2
    },
    name: 'Page2'
}];