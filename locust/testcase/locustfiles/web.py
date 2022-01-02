import uuid

from common.auth import login

from locust import HttpUser, between, task


class WebUser(HttpUser):
    """
    Http 测试
    """

    # 两次测试间的间隔时间
    wait_time = between(0.5, 10)

    def on_start(self):
        login(self.client)  # 登录

    @task
    def index(self):
        """
        测试 / 页面
        """
        # 访问页面
        with self.client.get('/', catch_response=True) as resp:
            # 下面这一步可以省略，非正确的 http 返回码会自动报告错误
            if resp.status_code != 200:  # 判断返回类型
                resp.failure("Get index failed")  # 报告错误
