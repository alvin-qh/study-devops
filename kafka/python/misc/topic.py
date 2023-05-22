import conf
from confluent_kafka.admin import AdminClient, NewTopic


def create_topic_if_not_exists(
    topic_name: str,
    num_partitions=3,
    replication_factor=2,
) -> None:
    """
    如果一个主题不存在, 则创建该主题

    Args:
    - `topic_name` (`str`): 主题名称
    - `num_partitions` (`int`, optional): 主题分区数. Defaults to `3`.
    - `replication_factor` (`int`, optional): 每分区副本数. Defaults to `2`.
    """
    # 创建管理端对象, 用于对 Kafka 进行管理操作
    admin_client = AdminClient({
        "bootstrap.servers": conf.BOOTSTRAP_SERVERS,  # 指定 Broker 地址
    })

    # 列举指定的主题 (如果 topic 参数为 None, 则列举所有主题)
    # 返回的结果为 ClusterMetadata 对象, 记录集群元数据, 其中的 topics 属性为主题字典
    meta = admin_client.list_topics(topic=topic_name, timeout=5.0)
    assert topic_name in meta.topics

    # 根据主题名称从主题字典中获取对应主题对象
    topic = meta.topics[topic_name]

    # 判断该主题是否具备分区, 如果不具备, 说明该主题尚未创建
    if not topic.partitions:
        # 创建指定主题, 参数为 NewTopic 对象的列表
        admin_client.create_topics([
            NewTopic(
                topic=topic_name,  # 主题名称
                num_partitions=num_partitions,  # 主题分区数量
                replication_factor=replication_factor,  # 主题副本数量
            )
        ])
