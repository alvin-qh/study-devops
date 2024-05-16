import os

import conf
from kazoo.client import KazooClient

# 创建 zookeeper client 对象
zk = KazooClient(hosts=conf.HOSTS)


def setup_function() -> None:
    # 连接到 zookeeper
    zk.start()

    # 删除指定路径下的所有子节点
    # recursive 表示是否递归的删除下属所有子路径和子节点
    zk.delete("/alvin", recursive=True)


def teardown_function() -> None:
    zk.stop()


def test_transaction() -> None:
    """
    Zookeeper 的 transaction 即批处理，要求要么一次全部成功，要么一次全部失败
    在多并发情况下，要同时设置多个结点的值，可以使用事务，将批任务作为一个原子提交
    """

    path = "/alvin/study/zk"
    zk.ensure_path(path)

    path_a = os.path.join(path, "a")
    zk.create(path_a)
    zk.set(path_a, b"a")

    path_b = os.path.join(path, "b")

    trans = zk.transaction()  # 启动事务
    trans.check(path_a, version=1)  # 判断 节点 a 的版本是否为 1
    trans.create(path_b, b"b")  # 设置 节点 b 的值
    rs = trans.commit()  # 提交事务，返回一个数组，表示事务中每个命令的执行结果

    assert rs[0]
    assert rs[1] == path_b

    val, _ = zk.get(path_b)
    assert val == b"b"
