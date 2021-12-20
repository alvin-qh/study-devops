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
