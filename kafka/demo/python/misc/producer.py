import socket
from typing import Any, Dict, Optional

import conf
from confluent_kafka import Producer


def create_producer(ext_props: Optional[Dict[str, Any]] = None) -> Producer:
    """
    创建生产者对象

    Args:
    - `ext_props` (`Optional[Dict[str, Any]]`, optional): 附件的配置项. Defaults to `None`.

    Returns:
        Producer: 生产者对象
    """
    props = {
        "client.id": socket.gethostname(),  # 定义客户端 ID 为当前主机的主机名
        "bootstrap.servers": conf.BOOTSTRAP_SERVERS,  # 指定 Broker 地址
    }

    if ext_props:
        # 附加扩展设置项
        props.update(ext_props)

    return Producer(props)
