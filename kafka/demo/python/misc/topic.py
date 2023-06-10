import logging
import conf
from confluent_kafka import KafkaError, KafkaException
from confluent_kafka.admin import AdminClient, NewTopic


# 日志对象
logger = logging.getLogger(__name__)


def create_topic(
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

    # 创建指定主题, 参数为 NewTopic 对象的列表
    # 返回结果为一个字典对象, Key 为要创建的主题名称, Value 为主题创建的 Future 对象
    # Future 对象的 result 方法返回 None 表示创建成功
    result = admin_client.create_topics(
        [
            NewTopic(
                topic=topic_name,  # 主题名称
                num_partitions=num_partitions,  # 主题分区数量
                replication_factor=replication_factor,  # 主题副本数量
            )
        ],
        operation_timeout=5.0,
        validate_only=False,
    )

    # 确认主题创建成功
    assert topic_name in result

    try:
        assert result[topic_name].result(timeout=5.0) is None
    except KafkaException as e:
        if e.args[0].code() != KafkaError.TOPIC_ALREADY_EXISTS:
            raise e

        logger.error(e)
