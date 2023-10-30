import logging
import random

from locust.env import Environment
from locust.runners import MasterRunner

from locust import User, constant, events, task


@events.test_start.add_listener
def on_test_start(environment: Environment, **kwargs):
    """
    事件回调
    当测试启动时执行一次
    """
    logging.info("A new test is starting")


@events.test_stop.add_listener
def on_test_stop(environment: Environment, **kwargs):
    """
    事件回调
    当测试停止时执行一次
    """
    logging.info("A new test is ending")


@events.init.add_listener
def on_test_init(environment: Environment, **kwargs):
    """
    事件回调
    当测试初始化完毕后执行一次
    """
    if isinstance(environment.runner, MasterRunner):
        logging.info("I'm on master node")
    else:
        logging.info("I'm on a worker or standalone node")


class SimpleUser(User):
    """
    非 http 基本测试
    """

    # 两次测试间的间隔时间
    wait_time = constant(1)

    def __init__(self, env: Environment):
        super().__init__(env)
        self._failure_rate = 0  # 定义失败率

    def on_start(self):
        """
        启动测试回调
        """
        self._failure_rate = random.randint(1, 10)  # 计算失败率
        logging.info(
            f"one tasks started, failure rate is: {self._failure_rate / 10 * 100}%"
        )

    def on_stop(self):
        """
        停止测试回调
        """
        logging.info("one tasks stop")

    """
    task1 和 task2 被执行的比例为为 3/6 = 1/2
    """

    @task(3)
    def task1(self):
        resp_time = random.randint(100, 10000)  # 随机计算响应时间
        resp_length = random.randint(100, 2000)  # 随机计算响应长度

        # 发起事件通知，本次测试成功
        events.request_success.fire(
            request_type="demo",
            name="task1",
            response_time=resp_time,
            response_length=resp_length,
        )

    @task(6)
    def task2(self):
        resp_time = random.randint(100, 10000)
        resp_length = random.randint(100, 2000)

        if random.randint(0, self._failure_rate) == 0:
            # 发起事件通知，本次测试失败
            events.request_failure.fire(
                request_type="demo",
                name="task2",
                response_time=resp_time,
                response_length=resp_length,
                exception=EOFError()  # 失败原因
            )
        else:
            # 发起事件通知，本次测试成功
            events.request_success.fire(
                request_type="demo",
                name="task2",
                response_time=resp_time,
                response_length=resp_length
            )
