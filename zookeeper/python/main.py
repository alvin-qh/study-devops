import atexit
import os
import sys
import threading
import time
from typing import Dict, List

from kazoo.client import KazooClient
from kazoo.exceptions import NodeExistsError

MODE = "RO"

# 创建一个 Zookeeper 客户端对象
#   hosts     表示要连接的服务器地址，如果要连接一个集群，则可以使用多个地址
#   read_only 表示是否以只读方式连接
zk = KazooClient(hosts="127.0.0.1:2181,127.0.0.1:2182",
                 read_only=True if MODE == "RO" else False)
# 连接 Zookeeper 客户端
zk.start()


def exit_handler():
    """
    进程结束后，断开 zookeeper 连接
    """
    zk.stop()


atexit.register(exit_handler)


def create_node(name: str) -> None:
    """
    创建 zookeeper 节点
    """
    try:
        # 根据路径创建节点并给予初始化值
        r = zk.create(name, b"Created")
        print(f"""zk node "{name}" was created success, path={r}""")
    except NodeExistsError:  # 节点已存在
        print(f"""zk node "{name}" already exists""", file=sys.stderr)

    # 生成一个子节点路径
    name = os.path.join(name, "ensure")

    # ensure_path，如果该路径节点存在，则清空 value，否则创建一个空节点
    if zk.ensure_path(name):
        print(f"""zk node "{name}" was ensured""")
    else:
        print(f"""zk node "{name}" was not ensured""")


def check_node(name: str) -> None:
    """
    检查节点是否存在
    """
    r = zk.exists(name)
    if r:
        print(f"""zk node "{name}" was exists""")
    else:
        print(f"""zk node "{name}" was not exists""")


def get_node_value(name: str) -> None:
    """
    根据路径获取该节点的值
    """

    # 根据路径获取节点，返回节点的值和节点状态
    data, state = zk.get(name)
    if data:
        print(f"""value of zk node "{name}" is {data}""")
        print(f"""state of zk node "{name}" is {state}""")

    # 根据路径获取所有子节点名称
    for node in zk.get_children(name):
        node = os.path.join(name, node)  # 将子节点名称和父节点路径连接成子节点路径
        data, state = zk.get(node)  # 根据子节点路径获取子节点值和状态
        print(
            f"""child of zk node "{name}" is "{node}", and value is "{data}" """
        )
        print(
            f"""child of zk node "{name}" is "{node}", and state is "{state}" """
        )


def update_node_value(kv: Dict[str, bytes]) -> None:
    """
    根据节点路径，更新节点的值
    """
    for k, v in kv.items():
        state = zk.set(k, v)  # 更新节点的纸
        if state is None:
            print(f"""zk node "{k}" could not updated""", file=sys.stderr)
        else:
            print(f"""zk node "{k}" was updated, state is "{state}" """)


def use_distributed_lock(lock_name: str) -> None:
    """
    使用 zookeeper 进行分布式锁
    """

    # 删除已存在的锁
    for lock_node in zk.get_children(lock_name) or []:
        zk.delete(os.path.join(lock_name, lock_node))

    def locked_append(nums: List[int]) -> None:
        """
        在锁定状态下向数组添加内容
        """
        lock = zk.Lock(lock_name, "_distributed_lock")  # 声明锁
        with lock:  # 进入锁
            for n in range(1, 10):
                nums.append(n)
                time.sleep(0.1)  # 切换线程

    nums = []

    # 两个线程
    threads = [
        threading.Thread(target=locked_append, kwargs={"nums": nums}),
        threading.Thread(target=locked_append, kwargs={"nums": nums}),
    ]

    # 启动线程
    for t in threads:
        t.start()

    # 等待线程结束
    for t in threads:
        t.join()

    # 输出结果，数组中的内容应该是两个线程依次执行的结果
    print(f"""The list locked by "{lock_name}" is {nums}""")


def main():
    node_name = "/alvin/study/zk"

    create_node(node_name)
    check_node(node_name)
    get_node_value(node_name)
    update_node_value({
        node_name: b"Updated",
        os.path.join(node_name, "ensure"): b"Updated"
    })
    get_node_value(node_name)

    lock_name = "/alvin/study/zk/locked"

    use_distributed_lock(lock_name)


if __name__ == "__main__":
    main()
