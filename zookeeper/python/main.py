import atexit
import os
import sys
from typing import Dict

from kazoo.client import KazooClient
from kazoo.exceptions import NodeExistsError, NoNodeError

MODE = "RO"

zk = KazooClient(hosts="127.0.0.1:2181,127.0.0.1:2182",
                 read_only=True if MODE == "RO" else False)
zk.start()


def exit_handler():
    zk.stop()


atexit.register(exit_handler)


def create_node(name: str) -> None:
    """
    Create a new node or Ensure a node if it exists
    """
    try:
        r = zk.create(name, b"Created")
        print(f"""zk node "{name}" was created success, path={r}""")
    except NoNodeError as err:
        print(f"""zk node "{name}" not exists""", file=sys.stderr)
    except NodeExistsError as err:
        print(f"""zk node "{name}" already exists""", file=sys.stderr)

    name = os.path.join(name, "ensure")

    if zk.ensure_path(name):
        print(f"""zk node "{name}" was ensured""")
    else:
        print(f"""zk node "{name}" was not ensured""")


def check_node(name: str) -> None:
    """
    Check node if it exists
    """
    r = zk.exists(name)
    if r:
        print(f"""zk node "{name}" was exists""")
    else:
        print(f"""zk node "{name}" was not exists""")

    r = zk.exists(name)
    if r:
        print(f"""zk node "{name}" was exists""")
    else:
        print(f"""zk node "{name}" was not exists""")


def get_node_value(name: str) -> None:
    data, state = zk.get(name)
    if data:
        print(f"""value of zk node "{name}" is {data}""")
        print(f"""state of zk node "{name}" is {state}""")

    for node in zk.get_children(name):
        node = os.path.join(name, node)
        data, state = zk.get(node)
        print(
            f"""child of zk node "{name}" is "{node}", and value is "{data}" """
        )
        print(
            f"""child of zk node "{name}" is "{node}", and state is "{state}" """
        )


def update_node_value(kv: Dict[str, bytes]) -> None:
    for k, v in kv.items():
        state = zk.set(k, v)
        if state is None:
            print(f"""zk node "{k}" could not updated""", file=sys.stderr)
        else:
            print(f"""zk node "{k}" was updated, state is "{state}" """)


def main():
    node_name = "/alvin/study/zk"

    create_node(node_name)
    check_node(node_name)
    get_node_value(node_name)
    update_node_value({
        node_name: b"Updated",
        os.path.join(node_name, "ensure"): b"Updated"
    })


if __name__ == "__main__":
    main()
