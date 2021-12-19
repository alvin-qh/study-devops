import os
import threading
import time
from typing import Callable, List

import conf
from kazoo.client import KazooClient

# 创建 zookeeper client 对象
zk = KazooClient(hosts=conf.HOSTS)


def setup_function():
    # 连接到 zookeeper
    zk.start()

    # 删除指定路径下的所有子节点
    # recursive 表示是否“递归的”删除下属所有子路径和子节点
    zk.delete("/alvin", recursive=True)


def teardown_function():
    zk.stop()


class ThreadGroup:
    """
    线程组类，将多个功能一致，参数相同的线程组织在一起
    """

    def __init__(self, target: Callable, *args, **kwargs) -> None:
        self._group = []  # 保存线程的数组
        self._target = target  # 线程执行函数
        self._args = args  # 线程参数
        self._kwargs = kwargs  # 线程参数

    def create_new(self) -> None:
        """
        创建新线程
        """
        t = threading.Thread(
            target=self._target, args=self._args or (), kwargs=self._kwargs or {}
        )
        self._group.append(t)

    def start_all(self) -> None:
        """
        启动所有线程
        """
        for t in self._group:
            t.start()

    def join(self) -> None:
        """
        等待所有线程执行完毕
        """
        for t in self._group:
            t.join()


def test_distributed_lock():
    """
    测试 zookeeper 用于分布式锁
    """

    def locked_append(lock_name: str, nums: List[int]) -> None:
        """
        在锁定状态下向数组添加内容
        """
        lock = zk.Lock(lock_name, "_distributed_lock")  # 声明锁
        with lock:  # 进入锁
            for n in range(1, 10):
                nums.append(n)
                time.sleep(0.1)  # 切换线程

    # 创建锁节点所在的路径
    path = "/alvin/study/zk"
    zk.ensure_path(path)

    nums: List[int] = []  # 保存结果的数组，三个线程产生的数据应该依次存储在该数组中

    # 创建线程组对象
    tg = ThreadGroup(
        target=locked_append,  # 设置线程函数
        lock_name=os.path.join(path, "lock"),  # 设置锁节点名称
        nums=nums  # 保存计算结果的数组
    )
    tg.create_new()  # 创建三个线程
    tg.create_new()
    tg.create_new()

    tg.start_all()  # 启动所有线程
    tg.join()

    # 结果中，三个线程产生的数据依次添加在数组中
    assert nums == [
        1, 2, 3, 4, 5, 6, 7, 8, 9,
        1, 2, 3, 4, 5, 6, 7, 8, 9,
        1, 2, 3, 4, 5, 6, 7, 8, 9,
    ]
