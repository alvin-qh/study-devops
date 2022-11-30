import json

import conf
import pytest
from kafka.admin import NewTopic
from kafka.errors import KafkaError, TopicAlreadyExistsError
from kafka.producer.future import FutureRecordMetadata

import kafka as ka

BOOTSTRAP_SERVERS = conf.HOSTS.split(",")


def setup_function():
    """
    测试开始
    """
    try:
        _create_new_topic()
    except TopicAlreadyExistsError:
        pass


def _create_new_topic():
    """
    创建新主题
    """
    # 新主题对象
    new_topic = NewTopic(
        conf.TOPIC,  # 主题名称
        num_partitions=5,  # 主题分区
        replication_factor=3,  # 主题副本
    )

    admin_client = ka.KafkaAdminClient(
        client_id=conf.CLIENT_ID,
        bootstrap_servers=BOOTSTRAP_SERVERS,
    )

    try:
        # 创建新主题
        admin_client.create_topics([new_topic])
    finally:
        admin_client.close()


def teardown_function():
    """
    测试结束
    关闭 Kafka 连接
    """

    admin_client = ka.KafkaAdminClient(
        client_id=conf.CLIENT_ID,
        bootstrap_servers=BOOTSTRAP_SERVERS,
    )

    try:
        # 删除主题
        admin_client.delete_topics([conf.TOPIC])
    except:
        pass
    finally:
        admin_client.close()


def test_topic():
    """
    测试主题操作
    """
    admin_client = ka.KafkaAdminClient(
        client_id=conf.CLIENT_ID,
       security_protocol="PLAINTEXT",
       bootstrap_servers=BOOTSTRAP_SERVERS,
    )

    try:
        # 列举所有的主题
        topics = admin_client.list_topics()
        assert conf.TOPIC in topics

        # 获取主题详情
        desc = admin_client.describe_topics([conf.TOPIC])
        assert desc[0]["topic"] == conf.TOPIC
        assert len(desc[0]["partitions"]) == 5
        assert desc[0]["partitions"][0]["partition"] == 0
        assert set(desc[0]["partitions"][0]["replicas"]) == {1, 2, 3}
    finally:
        admin_client.close()


def test_simple_record() -> None:
    """
    测试 producer 发送简单消息
    """
    value = b"Hello World!"

    # 连接 Kafka，创建 Producer
    producer = ka.KafkaProducer(
        client_id=conf.CLIENT_ID,
        bootstrap_servers=BOOTSTRAP_SERVERS,
    )

    try:
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

        producer.flush()  # 刷新 producer，将所有缓存的消息发送
    finally:
        producer.close()

    # 获取 Kafka，创建 Consumer
    consumer = ka.KafkaConsumer(
        conf.TOPIC,  # 消费信息的主题
        client_id=conf.CLIENT_ID,  # 客户端 ID
        group_id=conf.GROUP_ID,  # 消费组 ID
        bootstrap_servers=BOOTSTRAP_SERVERS,  # Broker 服务器列表
        auto_offset_reset="earliest",  # 自动重置 offset 到最早位置
    )

    try:
        # 从 consumer 读取记录
        for record in consumer:
            assert record.topic == conf.TOPIC  # 接收到的主题
            assert record.partition in {0, 1, 2, 3, 4}  # 接收到消息的分区
            assert record.value == value  # 接收到的值
            break
    finally:
        consumer.close()


def test_read_and_commit_manual():
    """
    接收消息并手动提交 offset
    即消息处理成功后，进行提交
    """
    value = b"Hello World!"

    # 连接 Kafka，创建 Producer
    producer = ka.KafkaProducer(
        client_id=conf.CLIENT_ID,
        bootstrap_servers=BOOTSTRAP_SERVERS,
    )

    try:
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

        producer.flush()  # 刷新 producer，将所有缓存的消息发送
    finally:
        producer.close()

    # 获取 Kafka，创建 Consumer
    consumer = ka.KafkaConsumer(
        conf.TOPIC,  # 消费信息的主题
        client_id=conf.CLIENT_ID,  # 客户端 ID
        group_id=conf.GROUP_ID,  # 消费组 ID
        bootstrap_servers=BOOTSTRAP_SERVERS,  # Broker 服务器列表
        auto_offset_reset="earliest",  # 自动重置 offset 到最早位置
        enable_auto_commit=False,
    )

    try:
        # 从 consumer 读取记录
        for record in consumer:
            assert record.topic == conf.TOPIC  # 接收到的主题
            assert record.partition in {0, 1, 2, 3, 4}  # 接收到消息的分区
            assert record.value == value  # 接收到的值

            consumer.commit()
            break
    finally:
        consumer.close()


def test_seek_consumer_offset():
    """
    测试移动 consumer 的 offset
    """
    value = b"Hello World!"

    # 连接 Kafka，创建 Producer
    producer = ka.KafkaProducer(
        client_id=conf.CLIENT_ID,
        bootstrap_servers=BOOTSTRAP_SERVERS,
    )

    try:
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

        producer.flush()  # 刷新 producer，将所有缓存的消息发送
    finally:
        producer.close()

    # 获取 Kafka，创建 Consumer
    consumer = ka.KafkaConsumer(
        client_id=conf.CLIENT_ID,  # 客户端 ID
        group_id=conf.GROUP_ID,  # 消费组 ID
        bootstrap_servers=BOOTSTRAP_SERVERS,  # Broker 服务器列表
    )

    try:
        # 设置主题和分区关系
        tp = ka.TopicPartition(topic=conf.TOPIC, partition=record.partition)

        # 将设定好的主题分区分配给 consumer
        consumer.assign([tp])

        # 移动 offset 表示从头读取
        consumer.seek_to_beginning()

        # 从 consumer 读取记录
        for record in consumer:
            assert record.topic == conf.TOPIC  # 接收到的主题
            assert record.partition in {0, 1, 2, 3, 4}  # 接收到消息的分区
            assert record.value == value  # 接收到的值
            break
    finally:
        consumer.close()


def test_kv_record():
    """
    测试 producer 发送 Key/Value 键值对消息
    """
    key = b'key'
    value = b"Hello World!"

    # 连接 Kafka，创建 Producer
    producer = ka.KafkaProducer(
        client_id=conf.CLIENT_ID,
        bootstrap_servers=BOOTSTRAP_SERVERS,
    )

    try:
        # 异步发送消息
        future: FutureRecordMetadata = producer.send(
            conf.TOPIC,
            key=key,
            value=value,
        )

        try:
            # 获取消息发送结果（异步）
            record = future.get(timeout=30)
        except KafkaError as err:
            pytest.fail(err)

        assert record.topic == conf.TOPIC  # 发送的主题
        assert record.partition in {0, 1, 2, 3, 4}  # 发送到的分区
        assert record.offset >= 0  # 发送分区的偏移量

        producer.flush()  # 刷新 producer，将所有缓存的消息发送
    finally:
        producer.close()

    # 获取 Kafka，创建 Consumer
    consumer = ka.KafkaConsumer(
        conf.TOPIC,  # 消费信息的主题
        client_id=conf.CLIENT_ID,  # 客户端 ID
        group_id=conf.GROUP_ID,  # 消费组 ID
        bootstrap_servers=BOOTSTRAP_SERVERS,  # Broker 服务器列表
        auto_offset_reset="earliest",  # 自动重置 offset 到最早位置
    )
    try:
        # 从 consumer 读取记录
        for record in consumer:
            assert record.topic == conf.TOPIC  # 接收到的主题
            assert record.partition in {0, 1, 2, 3, 4}  # 接收到消息的分区
            assert record.key == key  # 接收到的 key
            assert record.value == value  # 接收到的值
            break
    finally:
        consumer.close()


def test_json_deserializer():
    """
    测试 producer 发送编码信息和 consumer 解码信息
    """
    key = b'key'
    value = {
        "name": "Alvin",
        "action": "Hello",
    }

    # 连接 Kafka，创建 Producer
    producer = ka.KafkaProducer(
        client_id=conf.CLIENT_ID,
        bootstrap_servers=BOOTSTRAP_SERVERS,
        value_serializer=lambda x: (
            json.dumps(x).encode("utf8")
        ),  # 编码器，对 value 进行编码
    )

    try:
        # 异步发送消息
        future: FutureRecordMetadata = producer.send(
            conf.TOPIC,
            key=key,
            value=value,
        )

        try:
            # 获取消息发送结果（异步）
            record = future.get(timeout=30)
        except KafkaError as err:
            pytest.fail(err)

        assert record.topic == conf.TOPIC  # 发送的主题
        assert record.partition in {0, 1, 2, 3, 4}  # 发送到的分区
        assert record.offset >= 0  # 发送分区的偏移量

        producer.flush()  # 刷新 producer，将所有缓存的消息发送
    finally:
        producer.close()

    # 获取 Kafka，创建 Consumer
    consumer = ka.KafkaConsumer(
        conf.TOPIC,  # 消费信息的主题
        client_id=conf.CLIENT_ID,  # 客户端 ID
        group_id=conf.GROUP_ID,  # 消费组 ID
        bootstrap_servers=BOOTSTRAP_SERVERS,  # Broker 服务器列表
        auto_offset_reset="earliest",  # 自动重置 offset 到最早位置
        value_deserializer=lambda x: json.loads(
            x.decode("utf8"),
        )  # 解码器，对 value 进行解码
    )
    try:
        # 从 consumer 读取记录
        for record in consumer:
            assert record.topic == conf.TOPIC  # 接收到的主题
            assert record.partition in {0, 1, 2, 3, 4}  # 接收到消息的分区
            assert record.key == key  # 接收到的 key
            assert record.value == value  # 接收到的值
            break
    finally:
        consumer.close()
