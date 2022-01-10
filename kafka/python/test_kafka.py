import time

import conf
import pytest
from kafka.admin import NewTopic
from kafka.errors import KafkaError, TopicAlreadyExistsError
from kafka.producer.future import FutureRecordMetadata

import kafka as ka

admin_client: ka.KafkaAdminClient = None
producer: ka.KafkaProducer = None
consumer: ka.KafkaConsumer = None


def setup_function():
    """
    测试开始
    创建 Kafka 连接
    """
    global admin_client, producer, consumer

    addrs = conf.HOSTS.split(",")
    client_id = "test"

    admin_client = ka.KafkaAdminClient(
        client_id=client_id,
        bootstrap_servers=addrs,
    )

    try:
        _create_new_topic()
    except TopicAlreadyExistsError:
        pass

    # 连接 Kafka，创建 Producer
    producer = ka.KafkaProducer(
        client_id=client_id,
        bootstrap_servers=addrs,
    )

    # 获取 Kafka，创建 Consumer
    consumer = ka.KafkaConsumer(
        conf.TOPIC,
        client_id=client_id,
        group_id=conf.GROUP_ID,
        bootstrap_servers=addrs,
    )


def _create_new_topic():
    """
    创建新主题
    """
    # 新主题对象
    new_topic = NewTopic(
        conf.TOPIC,  # 主题名称
        num_partitions=5,  # 主题分区
        replication_factor=2,  # 主题副本
    )
    # 创建新主题
    admin_client.create_topics([new_topic])


def teardown_function():
    """
    测试结束
    关闭 Kafka 连接
    """
    producer.close()
    consumer.close()

    admin_client.delete_topics([conf.TOPIC])  # 删除主题
    admin_client.close()


def test_topic():
    """
    测试主题操作
    """
    # 列举所有的主题
    topics = admin_client.list_topics()
    assert conf.TOPIC in topics

    # 获取主题详情
    desc = admin_client.describe_topics([conf.TOPIC])
    assert desc[0]["topic"] == conf.TOPIC
    assert len(desc[0]["partitions"]) == 5
    assert desc[0]["partitions"][0]["partition"] == 0
    assert set(desc[0]["partitions"][0]["replicas"]) == {1, 2}


def test_producer_send():
    """
    测试 producer 发送消息
    """
    value = b"Hello World!"

    # 异步发送消息
    future: FutureRecordMetadata = producer.send(conf.TOPIC, value)

    try:
        # 获取消息发送结果（异步）
        record = future.get(timeout=30)
    except KafkaError as err:
        pytest.fail(err)

    assert record.topic == conf.TOPIC  # 发送的主题
    assert record.partition in {0, 1, 2, 3, 4}  # 发送到的分区
    assert record.offset >= 0  # 发送分区的偏移量

    time.sleep(1)

    for msg in consumer:
        assert msg.topic == conf.TOPIC  # 接收到的主题
        assert msg.partition in {0, 1, 2, 3, 4}  # 接收到消息的分区
        assert msg.value == value  # 接收到的值
        break
