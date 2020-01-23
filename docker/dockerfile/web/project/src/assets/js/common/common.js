import "../../css/common/common.less";
import moment from "moment";

const DATETIME_FORMAT = 'YYYY年MM月DD日 HH:mm:ss';

export const Times = {
    nowString() {
        return moment().format(DATETIME_FORMAT);
    }
};

export function runWith(name, cb) {
    const app = document.getElementById('app');
    if (app) {
        const role = app.getAttribute('app:name');
        if (role === name) {
            cb();
        }
    }
}