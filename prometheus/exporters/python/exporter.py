# 参考: https://github.com/prometheus/client_python

from metric import app, sched
from prometheus_client import make_wsgi_app
from werkzeug.middleware.dispatcher import DispatcherMiddleware

# if __name__ == "__main__":
#     start_http_server(8123)

# 加入 Prometheus client 中间件
app.wsgi_app = DispatcherMiddleware(app.wsgi_app, {
    "/metrics": make_wsgi_app()
})

if __name__ == "__main__":
    sched.start()

    # 启动测试服务器
    app.run(host="0.0.0.0", port=8123, debug=False)
