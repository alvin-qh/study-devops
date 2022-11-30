import os
import time
from typing import List

import conf
from kazoo.client import KazooClient, WatchedEvent
from kazoo.protocol.states import ZnodeStat

# 创建 zookeeper client 对象
zk = KazooClient(hosts=conf.HOSTS)


def setup_function():
    # 连接到 zookeeper
    zk.start()

    # 删除指定路径下的所有子节点
    # recursive 表示是否递归的删除下属所有子路径和子节点
    zk.delete("/alvin", recursive=True)


def teardown_function():
    zk.stop()


def test_low_level_watch_api():
    """
    低阶 API 监听节点变化
    """
    path = "/alvin/study"
    zk.ensure_path(path)  # 创建节点路径

    node = os.path.join(path, "zk")
    zk.create(node, b"created")  # 创建节点

    def node_watch(event: WatchedEvent) -> None:
        """
        节点发生变化时回调
        """
        assert event.type == "CHANGED"  # 确认事件类型
        assert event.path == node   # 确认节点路径
        assert event.state == "CONNECTED"  # 确认 client 状态

    # 获取节点并添加 watch 函数
    zk.get(node, watch=node_watch)

    # 更新节点值，此时 watch 函数会被调用
    zk.set(node, b"updated")


def test_high_level_watch_api():
    """
    高阶 API 监听节点变化
    """
    path = "/alvin/study"
    zk.ensure_path(path)  # 创建节点路径

    node = os.path.join(path, "zk")
    zk.create(node, b"created")  # 创建节点

    result = set()

    @zk.DataWatch(path="/alvin/study/zk")
    def data_watch(data: bytes, stat: ZnodeStat) -> None:
        """
        监听节点数据变化情况
        """
        assert data == b"created"
        result.add("zk")

    @zk.ChildrenWatch(path="/alvin/study/zk")
    def children_watch(children: List[str]) -> None:
        """
        监听子节点变化情况
        """
        result.update(children)

    # 增加子节点
    for n in range(1, 11):
        child_node = os.path.join(node, str(n))
        zk.create(child_node, b"created")

    while len(result) < 11:
        time.sleep(0.5)

    assert sorted(list(result)) == [
        "1", "10", "2", "3", "4", "5", "6", "7", "8", "9", "zk"
    ]


def test_connection_listener():
    """
    监听连接变化情况
    """

    def connection_listener(stat) -> None:
        """
        连接发生变化时的回调
        """
        assert stat == "LOST"

    # 添加连接变化监听
    zk.add_listener(connection_listener)
