import os

import conf
import pytest
from kazoo.client import KazooClient
from kazoo.exceptions import BadVersionError, NodeExistsError, NoNodeError
from kazoo.retry import KazooRetry

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


def test_create_node():
    """
    测试创建节点
    """

    # 节点 "/alvin/study/zk" 的路径为 "/alvin/study"，节点名称为 "zk"
    node = "/alvin/study/zk"

    # 创建节点，当节点不存在时，会抛出 NoNodeError 错误
    try:
        r = zk.create(node, b"Created")
        pytest.fail("Cannot run here")
    except NoNodeError:
        pass

    # 可以通过 makepath 参数确保当节点路径不存在时，创建该路径
    r = zk.create(node, b"Created", makepath=True)
    assert r == node

    # 获取节点值和状态
    r, state = zk.get(node)
    assert r == b"Created"  # 确认节点值
    assert state.version == 0  # 确认节点版本
    assert state.dataLength == 7  # 节点数据长度
    assert state.numChildren == 0  # 节点没有子节点

    # 当该节点已被创建，则再次创建会导致 NodeExistsError 错误
    try:
        r = zk.create(node, b"Created")
        pytest.fail("Cannot run here")
    except NodeExistsError:
        pass


def test_ensure_node():
    """
    确保节点路径存在
    注意，如果 ensure_path 的参数原本表示一个节点，则会将节点删除，作为一个路径
    """

    path = "/alvin/study"

    # 确保节点路径存在，如果不存在则创建该路径
    r = zk.ensure_path(path)
    assert r == path

    # 确认节点路径存在
    r = zk.exists(path)
    assert r is not None

    # 创建子节点
    node1 = os.path.join(path, "zk")
    r = zk.create(node1, b"node zk")

    # 创建子节点
    node2 = os.path.join(node1, "ensure")
    r = zk.create(node2, b"node ensure")

    r, _ = zk.get(node1)
    assert r == b"node zk"

    r, _ = zk.get(node2)
    assert r == b"node ensure"


def test_update_node():
    path = "/alvin/study"

    # 创建节点路径
    zk.ensure_path(path)

    # 创建节点
    node = os.path.join(path, "zk")
    zk.create(node, b"created")

    # 获取节点
    val, stat = zk.get(node)
    assert val == b"created"
    assert stat.version == 0  # 刚创建的节点版本为 0

    # 忽略版本更新节点值
    r = zk.set(node, b"updated")
    assert r.version == 1  # 更新后节点版本为 1

    # 获取更新后的节点
    val, stat = zk.get(node)
    assert val == b"updated"
    assert stat.version == 1

    # 使用错误的版本无法正确更新节点
    # 所以版本号可以作为“乐观锁”，保证更新节点时没有别的进程更新节点
    try:
        zk.set(node, b"update again", version=0)
        pytest.fail("can not run here")
    except BadVersionError:
        pass

    # 版本号正确即可正确的更新节点
    r = zk.set(node, b"update again", version=stat.version)
    assert r.version == 2

    # 获取更新后的节点
    val, stat = zk.get(node)
    assert val == b"update again"
    assert stat.version == 2  # 此时节点版本号为 2


def test_child_node():
    path = "/alvin/study"

    # 创建节点路径
    zk.ensure_path(path)

    # 在指定路径创建 10 个节点
    for n in range(1, 11):
        child_node = os.path.join(path, str(n))
        zk.create(child_node, str(n).encode())

    # 通过指定路径获取子节点
    children = zk.get_children(path)
    assert len(children) == 10  # 获取到 10 个子节点

    # 遍历子节点
    for n, child in enumerate(children):
        assert child == f"{n+1}"  # 确认子节点名称

        # 获取的子节点名称并不带节点路径，需要进行拼装

        # 获取子节点完整路径
        child_node = os.path.join(path, child)

        # 获取子节点值
        val, stat = zk.get(child_node)
        assert val.decode() == str(n+1)
        assert stat.version == 0


def test_retry():
    """
    retry 可以在失败后自动重试，保证命令执行成功
    """
    path = "/alvin/study"

    # 创建路径，失败后重试
    r = zk.retry(zk.ensure_path, path=path)
    assert r == path

    path = os.path.join(path, "zk")

    # 自定义重试规则，创建节点
    kr = KazooRetry(max_tries=3, ignore_expire=False)
    r = kr(zk.create, path, b"created")
    assert r == path
