import logging
from typing import Any, Dict, List, Optional, Tuple

import conf
from confluent_kafka import (Consumer, KafkaError, KafkaException, Message,
                             TopicPartition)

from .message import MessageData

# 日志对象
logger = logging.getLogger(__name__)


def create_consumer(
    topic_name: str,
    group_id: str,
    ext_props: Optional[Dict[str, Any]] = None,
) -> Consumer:
    """
    创建消费者对象

    注意, 每次执行测试后, 都会导致 Kafka 对相关主题的分区进行再平衡操作, 这里为了方便测试,
    将 Broker 配置的 `leader.imbalance.check.interval.seconds` 设置为 `10s`,
    否则会导致每次测试启动时间较长 (等待再平衡操作)

    Args:
    - `topic_name` (`str`): 主题名称
    - `group_id` (`str`): 组 ID
    - `ext_props` (`Optional[Dict[str, Any]`], optional): 附加的消费者设置项. Defaults to `None`.

    Returns:
        Consumer: 消费者对象
    """
    props = {
        "bootstrap.servers": conf.BOOTSTRAP_SERVERS,  # 指定 Broker 地址
        "group.id": group_id,  # 指定消费者所在的消费组
        "enable.auto.commit": False,    # 关闭偏移量自动提交
        "auto.offset.reset": "earliest",  # 偏移量重置选项, 可选值包括:
                                          # smallest, earliest, beginning, largest,
                                          # latest, end, error
        "enable.partition.eof": True
    }

    if ext_props:
        # 添加扩展配置项
        props.update(ext_props)

    # 创建消费者对象
    consumer = Consumer(props)

    # 记录再平衡后分配的分区列表
    assigned_partitions: Optional[List[TopicPartition]] = None

    def on_assign(
        rebalanced_consumer: Consumer,
        partitions: List[TopicPartition],
    ) -> None:
        """
        当发生再平衡且消费者分配到指定主题的新分区时被调用, 并传入新的分区列表

        Args:
        - `rebalanced_consumer` (`Consumer`): 发生再平衡的消费者对象
        - `partitions` (`List[TopicPartition]`): 新分配的分区列表, 如果未分配新分区, 则为空
        """
        # 确认消费者对象
        assert rebalanced_consumer == consumer

        # 确认分配的分区列表不为空, 即至少分配到一个分区
        # 正常情况下应该是 3 个分区, 但不排除多次测试导致的再平衡过程中, 先分配 3 个以下的分区,
        # 在后续的再平衡继续分配新的分区
        assert len(partitions) >= 1

        # 确认分区的主题
        for p in partitions:
            assert p.topic == topic_name

        nonlocal assigned_partitions
        assigned_partitions = partitions

    def on_revoke(
        rebalanced_consumer: Consumer,
        partitions: List[TopicPartition],
    ) -> None:
        """
        当发生再平衡且消费者放弃指定主题的分区时被调用, 传入被放弃的分区列表

        Args:
        - `rebalanced_consumer` (`Consumer`): 发生再平衡的消费者对象
        - `partitions` (`List[TopicPartition]`): 主动放弃的分区列表
        """
        # 确认消费者对象
        assert rebalanced_consumer == consumer

        # 确认放弃的分区即为分配的分区
        assert partitions == assigned_partitions

        # 分区被放弃前提交一次偏移量
        consumer.commit(asynchronous=False)

    def on_lost(
        rebalanced_consumer: Consumer,
        partitions: List[TopicPartition],
    ) -> None:
        """
        会在进行意外的再平衡并且参数集合中的分区已经有新的所有者的情况下被调用

        Args:
        - `rebalanced_consumer` (`Consumer`): 发生再平衡的消费者对象
        - `partitions` (`List[TopicPartition]`): 丢失的分区列表
        """
        assert rebalanced_consumer == consumer

    # 指定消费者订阅主题
    consumer.subscribe(
        [topic_name],  # 要定义的主题列表
        on_assign=on_assign,  # 再平衡后分配新分区的回调
        on_revoke=on_revoke,  # 再平衡后失去分区的回调
        on_lost=on_lost,  # 意外导致丢失分区的回调
    )

    return consumer


def poll_and_assert(  # NOSONAR
    consumer: Consumer,
    expected_messages: Optional[List[MessageData]] = None,
    commit=True,
) -> List[Message]:
    """
    从消费者读取消息并对消息进行断言

    Args:
    - `consumer` (`Consumer`): 消费者对象
    - `msg_data` (`Optional[MessageData]`, optional): 消息元组. Defaults to `None`.
    - `commit` (`bool`, optional): 是否提交偏移量. Defaults to `True`.

    Returns:
    - Message: 消息对象
    """

    # 最后读取的消息对象
    messages: List[Message] = []

    if expected_messages is None:
        expected_messages = []

    # 记录已经到达末尾的分区字典
    # Key 为主题 + 分区, Value 为 Boolean 类型
    eof: Dict[Tuple[str, int], bool] = {}

    # 读取消息, 直到将所分配分区的消息读完
    while 1:
        # 读取消息, 超时时间 2s
        msg = consumer.poll(timeout=2.0)

        if msg:
            # 获取消息的主题, 分区和错误信息
            topic, partition, err = msg.topic(), msg.partition(), msg.error()
            if err:
                # 如果已经到达分区结尾, 则退出循环, 否则抛出异常
                if err.code() != KafkaError._PARTITION_EOF:
                    raise KafkaException(err)

                logger.error(err)

                # 记录到达末尾的主题和分区
                eof[(topic, partition)] = True

                # 判断如果所有主题分区都到达末尾, 则结束循环
                if len(eof) == len(consumer.assignment()):
                    break

                continue

            # 收到消息, 将指定主题分区的 EOF 标记取消
            eof.pop((topic, partition), None)

            if expected_messages and len(messages) == len(expected_messages):
                messages = messages[1:]

            # 记录最后一条消息
            messages.append(msg)

    assert messages

    if commit:
        try:
            # 提交偏移量 (同步方式)
            consumer.commit(asynchronous=False)
        except KafkaException as e:
            if e.args[0].code() == KafkaError._NO_OFFSET:
                logger.error(e)
            else:
                raise e

    # 获取最后一条消息
    last_msg = messages[-1]

    # 确认获取的消息无错误
    assert last_msg.error() is None

    # 确认获取消息的 Key 和 Value
    if expected_messages:
        for m, em in zip(messages, expected_messages):
            assert m.key() == em.key
            assert m.value() == em.value

    return messages


def close_consumer(consumer: Consumer) -> None:
    """
    关闭消费者对象

    Args:
    - `consumer` (`Consumer`): 消费者对象

    Raises:
    - `e`: 关闭时发生的异常
    """
    try:
        consumer.close()
    except KafkaException as e:
        logger.error(e)
        if e.args[0].code() != KafkaError._NO_OFFSET:
            raise e
