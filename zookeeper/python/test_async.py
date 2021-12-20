import os

import conf
from gevent.lock import BoundedSemaphore
from kazoo.client import KazooClient
from kazoo.handlers.gevent import AsyncResult, SequentialGeventHandler

# 创建 zookeeper client 对象
zk = KazooClient(hosts=conf.HOSTS, handler=SequentialGeventHandler())


def setup_function():
    # 连接到 zookeeper
    event = zk.start_async()
    event.wait(timeout=30)

    # 删除指定路径下的所有子节点
    # recursive 表示是否“递归的”删除下属所有子路径和子节点
    zk.delete("/alvin", recursive=True)


def teardown_function():
    zk.stop()


def test_create_node_async():
    """
    测试创建节点
    """

    sem = BoundedSemaphore()  # 表示工作完毕的信号量

    def ensure_path_callback(async_obj: AsyncResult) -> None:
        """
        路径创建后的回调
        """
        if not async_obj.successful():  # 判断路径是否创建成功
            return

        path = async_obj.get()  # 获取路径创建结果

        # 异步创建节点
        node_name = os.path.join(path, "zk")  # 拼装节点路径

        event = zk.create_async(node_name, b"created")
        event.rawlink(create_callback)  # 连接回调函数，处理节点创建消息

    created_node = ""  # 用来保存节点创建结果

    def create_callback(async_obj: AsyncResult) -> None:
        """
        节点创建后的回调
        """
        nonlocal created_node

        if not async_obj.successful():  # 判断节点是否创建成功
            return

        created_node = async_obj.get()  # 获取节点创建结果

        # 工作完成，释放信号量
        sem.release()

    path = "/alvin/study"

    # 异步创建节点
    event = zk.ensure_path_async(path)
    sem.acquire()  # 占用信号量，直到工作完成

    event.rawlink(ensure_path_callback)  # 连接回调函数，处理路径创建消息

    sem.wait()  # 等待工作完成

    expected_node = os.path.join(path, "zk")
    assert created_node == expected_node

    # 检验节点是否创建成功
    val, stat = zk.get(expected_node)
    assert val == b"created"
    assert stat.version == 0
