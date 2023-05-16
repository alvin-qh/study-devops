import json
import logging
import socket
import sys

import conf
import pytest
from confluent_kafka import KafkaException, Producer
from confluent_kafka.admin import AdminClient, NewTopic

logger = logging.getLogger(__name__)

admin_client = AdminClient({
    "bootstrap.servers": conf.BOOTSTRAP_SERVERS,
})

producer = Producer({
    "client.id": socket.gethostname(),
    "bootstrap.servers": conf.BOOTSTRAP_SERVERS,
})


def setup_module() -> None:
    """
    测试开始
    """
    try:
        # 创建新主题
        topic_futures = admin_client.create_topics([
            NewTopic(
                topic=conf.TOPIC,
                num_partitions=conf.NUM_PARTITIONS,
                replication_factor=conf.REPLICATION_FACTOR
            )
        ])

        assert conf.TOPIC in topic_futures

        future = topic_futures[conf.TOPIC]
        assert future.result(timeout=5) is None
    except KafkaException as e:
        logger.error("Cannot create topic, reason is: %s", e)


def teardown_module() -> None:
    """
    测试结束
    """
    # 删除主题
    # admin_client.delete_topics([conf.TOPIC])


def test_topic() -> None:
    """
    测试主题操作
    """
    # 列举指定的主题
    # topic 参数为指定的主题, 如果省略则表示所有主题
    topics = admin_client.list_topics(topic=conf.TOPIC, timeout=5)

    # 确认集群 id, 和 cluster.id 配置一致
    assert topics.cluster_id == "abcdefghijklmnopqrstug"  # cspell: disable-line

    # 该主题 Broker 数量, 共启动 3 个 Broker 节点
    # 注意, topics.brokers 不是一个数组, 而是 Key 为数字的字典
    assert len(topics.brokers) == 3

    # 确认 3 个 Broker 的情况
    # 获取第一个 Broker 对象
    broker1 = topics.brokers[1]
    # 确认 Broker 的主机名和端口号
    assert broker1.host == "localhost" and broker1.port == 19092

    # 下同
    broker2 = topics.brokers[2]
    assert broker2.host == "localhost" and broker2.port == 19093

    broker3 = topics.brokers[3]
    assert broker3.host == "localhost" and broker3.port == 19094

    # 获取结果中的 Topic 总数, 为 1, 表示本次获取到 1 个主题
    assert len(topics.topics) == 1
    assert conf.TOPIC in topics.topics

    # 根据主题名称获取主题对象
    topic = topics.topics[conf.TOPIC]
    assert topic.topic == conf.TOPIC

    # 确认该主题具有 3 个分区 (分区 id 分别为 0, 1, 2)
    assert len(topic.partitions) == conf.NUM_PARTITIONS

    # 确认 3 个分区的情况
    # 获取第一个分区对象
    partition1 = topic.partitions[0]
    # 确认分区 id
    assert partition1.id == 0
    # 确认分区副本为 2 个
    assert len(partition1.replicas) == conf.REPLICATION_FACTOR
    # 确认分区 Leader 是两个副本之一
    assert partition1.leader in partition1.replicas
    # 确认 ISR 集合里面包含所有副本 (因为副本数量只有 2 个, 所以和 ISR 集合一致)
    assert partition1.isrs == partition1.replicas

    # 下同

    partition2 = topic.partitions[1]
    assert partition2.id == 1
    assert partition2.isrs == partition2.replicas
    assert partition2.leader in partition2.replicas

    partition3 = topic.partitions[2]
    assert partition3.id == 2
    assert partition3.isrs == partition3.replicas
    assert partition3.leader in partition3.replicas


# def test_simple_record() -> None:
#     """
#     测试 producer 发送简单消息
#     """
#     value = b"Hello World!"

#     # 连接 Kafka，创建 Producer
#     producer = ka.KafkaProducer(
#         client_id=conf.CLIENT_ID,
#         bootstrap_servers=BOOTSTRAP_SERVERS,
#     )

#     try:
#         # 异步发送消息
#         future = producer.send(conf.TOPIC, value)

#         try:
#             # 获取消息发送结果（异步）
#             record = future.get(timeout=5)
#         except KafkaError as err:
#             pytest.fail(err)

#         assert record.topic == conf.TOPIC  # 发送的主题
#         assert record.partition in {0, 1, 2, 3, 4}  # 发送到的分区
#         assert record.offset >= 0  # 发送分区的偏移量

#         producer.flush()  # 刷新 producer，将所有缓存的消息发送
#     finally:
#         producer.close()

#     # 获取 Kafka，创建 Consumer
#     consumer = ka.KafkaConsumer(
#         conf.TOPIC,  # 消费信息的主题
#         client_id=conf.CLIENT_ID,  # 客户端 ID
#         group_id=conf.GROUP_ID,  # 消费组 ID
#         bootstrap_servers=BOOTSTRAP_SERVERS,  # Broker 服务器列表
#         auto_offset_reset="earliest",  # 自动重置 offset 到最早位置
#     )

#     try:
#         # 从 consumer 读取记录
#         for record in consumer:
#             assert record.topic == conf.TOPIC  # 接收到的主题
#             assert record.partition in {0, 1, 2, 3, 4}  # 接收到消息的分区
#             assert record.value == value  # 接收到的值
#             break
#     finally:
#         consumer.close()


# def test_read_and_commit_manual() -> None:
#     """
#     接收消息并手动提交 offset
#     即消息处理成功后，进行提交
#     """
#     value = b"Hello World!"

#     # 连接 Kafka，创建 Producer
#     producer = ka.KafkaProducer(
#         client_id=conf.CLIENT_ID,
#         bootstrap_servers=BOOTSTRAP_SERVERS,
#     )

#     try:
#         # 异步发送消息
#         future: FutureRecordMetadata = producer.send(conf.TOPIC, value)

#         try:
#             # 获取消息发送结果（异步）
#             record = future.get(timeout=30)
#         except KafkaError as err:
#             pytest.fail(err)

#         assert record.topic == conf.TOPIC  # 发送的主题
#         assert record.partition in {0, 1, 2, 3, 4}  # 发送到的分区
#         assert record.offset >= 0  # 发送分区的偏移量

#         producer.flush()  # 刷新 producer，将所有缓存的消息发送
#     finally:
#         producer.close()

#     # 获取 Kafka，创建 Consumer
#     consumer = ka.KafkaConsumer(
#         conf.TOPIC,  # 消费信息的主题
#         client_id=conf.CLIENT_ID,  # 客户端 ID
#         group_id=conf.GROUP_ID,  # 消费组 ID
#         bootstrap_servers=BOOTSTRAP_SERVERS,  # Broker 服务器列表
#         auto_offset_reset="earliest",  # 自动重置 offset 到最早位置
#         enable_auto_commit=False,
#     )

#     try:
#         # 从 consumer 读取记录
#         for record in consumer:
#             assert record.topic == conf.TOPIC  # 接收到的主题
#             assert record.partition in {0, 1, 2, 3, 4}  # 接收到消息的分区
#             assert record.value == value  # 接收到的值

#             consumer.commit()
#             break
#     finally:
#         consumer.close()


# def test_seek_consumer_offset() -> None:
#     """
#     测试移动 consumer 的 offset
#     """
#     value = b"Hello World!"

#     # 连接 Kafka，创建 Producer
#     producer = ka.KafkaProducer(
#         client_id=conf.CLIENT_ID,
#         bootstrap_servers=BOOTSTRAP_SERVERS,
#     )

#     try:
#         # 异步发送消息
#         future: FutureRecordMetadata = producer.send(conf.TOPIC, value)

#         try:
#             # 获取消息发送结果（异步）
#             record = future.get(timeout=30)
#         except KafkaError as err:
#             pytest.fail(err)

#         assert record.topic == conf.TOPIC  # 发送的主题
#         assert record.partition in {0, 1, 2, 3, 4}  # 发送到的分区
#         assert record.offset >= 0  # 发送分区的偏移量

#         producer.flush()  # 刷新 producer，将所有缓存的消息发送
#     finally:
#         producer.close()

#     # 获取 Kafka，创建 Consumer
#     consumer = ka.KafkaConsumer(
#         client_id=conf.CLIENT_ID,  # 客户端 ID
#         group_id=conf.GROUP_ID,  # 消费组 ID
#         bootstrap_servers=BOOTSTRAP_SERVERS,  # Broker 服务器列表
#     )

#     try:
#         # 设置主题和分区关系
#         tp = ka.TopicPartition(topic=conf.TOPIC, partition=record.partition)

#         # 将设定好的主题分区分配给 consumer
#         consumer.assign([tp])

#         # 移动 offset 表示从头读取
#         consumer.seek_to_beginning()

#         # 从 consumer 读取记录
#         for record in consumer:
#             assert record.topic == conf.TOPIC  # 接收到的主题
#             assert record.partition in {0, 1, 2, 3, 4}  # 接收到消息的分区
#             assert record.value == value  # 接收到的值
#             break
#     finally:
#         consumer.close()


# def test_kv_record() -> None:
#     """
#     测试 producer 发送 Key/Value 键值对消息
#     """
#     key = b'key'
#     value = b"Hello World!"

#     # 连接 Kafka，创建 Producer
#     producer = ka.KafkaProducer(
#         client_id=conf.CLIENT_ID,
#         bootstrap_servers=BOOTSTRAP_SERVERS,
#     )

#     try:
#         # 异步发送消息
#         future: FutureRecordMetadata = producer.send(
#             conf.TOPIC,
#             key=key,
#             value=value,
#         )

#         try:
#             # 获取消息发送结果（异步）
#             record = future.get(timeout=30)
#         except KafkaError as err:
#             pytest.fail(err)

#         assert record.topic == conf.TOPIC  # 发送的主题
#         assert record.partition in {0, 1, 2, 3, 4}  # 发送到的分区
#         assert record.offset >= 0  # 发送分区的偏移量

#         producer.flush()  # 刷新 producer，将所有缓存的消息发送
#     finally:
#         producer.close()

#     # 获取 Kafka，创建 Consumer
#     consumer = ka.KafkaConsumer(
#         conf.TOPIC,  # 消费信息的主题
#         client_id=conf.CLIENT_ID,  # 客户端 ID
#         group_id=conf.GROUP_ID,  # 消费组 ID
#         bootstrap_servers=BOOTSTRAP_SERVERS,  # Broker 服务器列表
#         auto_offset_reset="earliest",  # 自动重置 offset 到最早位置
#     )
#     try:
#         # 从 consumer 读取记录
#         for record in consumer:
#             assert record.topic == conf.TOPIC  # 接收到的主题
#             assert record.partition in {0, 1, 2, 3, 4}  # 接收到消息的分区
#             assert record.key == key  # 接收到的 key
#             assert record.value == value  # 接收到的值
#             break
#     finally:
#         consumer.close()


# def test_json_deserializer() -> None:
#     """
#     测试 producer 发送编码信息和 consumer 解码信息
#     """
#     key = b'key'
#     value = {
#         "name": "Alvin",
#         "action": "Hello",
#     }

#     # 连接 Kafka，创建 Producer
#     producer = ka.KafkaProducer(
#         client_id=conf.CLIENT_ID,
#         bootstrap_servers=BOOTSTRAP_SERVERS,
#         value_serializer=lambda x: (
#             json.dumps(x).encode("utf8")
#         ),  # 编码器，对 value 进行编码
#     )

#     try:
#         # 异步发送消息
#         future: FutureRecordMetadata = producer.send(
#             conf.TOPIC,
#             key=key,
#             value=value,
#         )

#         try:
#             # 获取消息发送结果（异步）
#             record = future.get(timeout=30)
#         except KafkaError as err:
#             pytest.fail(err)

#         assert record.topic == conf.TOPIC  # 发送的主题
#         assert record.partition in {0, 1, 2, 3, 4}  # 发送到的分区
#         assert record.offset >= 0  # 发送分区的偏移量

#         producer.flush()  # 刷新 producer，将所有缓存的消息发送
#     finally:
#         producer.close()

#     # 获取 Kafka，创建 Consumer
#     consumer = ka.KafkaConsumer(
#         conf.TOPIC,  # 消费信息的主题
#         client_id=conf.CLIENT_ID,  # 客户端 ID
#         group_id=conf.GROUP_ID,  # 消费组 ID
#         bootstrap_servers=BOOTSTRAP_SERVERS,  # Broker 服务器列表
#         auto_offset_reset="earliest",  # 自动重置 offset 到最早位置
#         value_deserializer=lambda x: json.loads(
#             x.decode("utf8"),
#         )  # 解码器，对 value 进行解码
#     )
#     try:
#         # 从 consumer 读取记录
#         for record in consumer:
#             assert record.topic == conf.TOPIC  # 接收到的主题
#             assert record.partition in {0, 1, 2, 3, 4}  # 接收到消息的分区
#             assert record.key == key  # 接收到的 key
#             assert record.value == value  # 接收到的值
#             break
#     finally:
#         consumer.close()
