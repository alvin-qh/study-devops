import logging
import socket
from typing import List, Optional

import conf
from confluent_kafka import (Consumer, KafkaError, KafkaException, Message,
                             Producer, TopicPartition)
from confluent_kafka.admin import AdminClient, NewTopic

logger = logging.getLogger(__name__)

# 可配置项参考: https://github.com/confluentinc/librdkafka/blob/master/CONFIGURATION.md

# 创建管理端对象, 用于对 Kafka 进行管理操作
admin_client = AdminClient({
    "bootstrap.servers": conf.BOOTSTRAP_SERVERS,  # 指定 Broker 地址
})

# 创建生产者对象
producer = Producer({
    "client.id": socket.gethostname(),  # 定义客户端 ID 为当前主机的主机名
    "bootstrap.servers": conf.BOOTSTRAP_SERVERS,  # 指定 Broker 地址
})


# 创建消费者对象
# 注意, 每次执行测试后, 都会导致 Kafka 对相关主题的分区进行再平衡操作, 这里为了方便测试,
# 将 Broker 配置的 leader.imbalance.check.interval.seconds 设置为 10s,
# 否则会导致每次测试启动时间较长 (等待再平衡操作)
consumer = Consumer({
    "bootstrap.servers": conf.BOOTSTRAP_SERVERS,  # 指定 Broker 地址
    "group.id": conf.GROUP_ID,  # 指定消费者所在的消费组
    "enable.auto.commit": False,  # 关闭偏移量自动提交
    "auto.offset.reset": "latest",  # 偏移量重置选项, 可选值包括:
                                    # smallest, earliest, beginning, largest,
                                    # latest, end, error
})

# 记录再平衡后分配的分区列表
assigned_partitions: Optional[List[TopicPartition]] = None


def on_assign(loc_consumer: Consumer, partitions: List[TopicPartition]) -> None:
    """
    当发生再平衡且消费者分配到指定主题的新分区时被调用, 并传入新的分区列表

    Args:
        loc_consumer (Consumer): 发生再平衡的消费者对象
        partitions (List[TopicPartition]): 新分配的分区列表, 如果未分配新分区, 则为空
    """
    # 确认消费者对象
    assert loc_consumer == consumer

    # 确认分配的分区列表不为空, 即至少分配到一个分区
    # 正常情况下应该是 3 个分区, 但不排除多次测试导致的再平衡过程中, 先分配 3 个以下的分区,
    # 在后续的再平衡继续分配新的分区
    assert len(partitions) >= 1

    # 确认分区的主题
    for p in partitions:
        assert p.topic == conf.TOPIC

    global assigned_partitions
    assigned_partitions = partitions


def on_revoke(loc_consumer: Consumer, partitions: List[TopicPartition]) -> None:
    """
    当发生再平衡且消费者放弃指定主题的分区时被调用, 传入被放弃的分区列表

    Args:
        loc_consumer (Consumer): 发生再平衡的消费者对象
        partitions (List[TopicPartition]): 主动放弃的分区列表
    """
    # 确认消费者对象
    assert loc_consumer == consumer

    # 确认放弃的分区即为分配的分区
    assert partitions == assigned_partitions

    # 分区被放弃前提交一次偏移量
    consumer.commit(asynchronous=False)


def on_lost(loc_consumer: Consumer, partitions: List[TopicPartition]) -> None:
    """
    会在进行意外的再平衡并且参数集合中的分区已经有新的所有者的情况下被调用

    Args:
        loc_consumer (Consumer): 发生再平衡的消费者对象
        partitions (List[TopicPartition]): 丢失的分区列表
    """
    assert loc_consumer == consumer


# 指定消费者订阅主题
consumer.subscribe(
    [conf.TOPIC],  # 要定义的主题列表
    on_assign=on_assign,  # 再平衡后分配新分区的回调
    on_revoke=on_revoke,  # 再平衡后失去分区的回调
    on_lost=on_lost,  # 意外导致丢失分区的回调
)


def setup_module() -> None:
    """
    启动测试, 创建所需的主题
    """
    try:
        # 创建新主题
        # 该方法返回一个 Key 为主题名称, Value 为 Future 对象的字典
        topic_futures = admin_client.create_topics([
            # 创建主题对象
            NewTopic(
                topic=conf.TOPIC,  # 主题名称
                num_partitions=conf.NUM_PARTITIONS,  # 主题分区数量
                replication_factor=conf.REPLICATION_FACTOR,  # 主题副本数量
            )
        ])

        # 确认返回了指定主题的 Future 对象
        assert conf.TOPIC in topic_futures

        # 根据主题名称获取对应的 Future 对象
        future = topic_futures[conf.TOPIC]
        # 等待 Future 对象执行完毕, 返回 None 表示成功
        assert future.result(timeout=5) is None
    except KafkaException as e:
        # 获取异常对象中的 KafkaError 对象
        arg = e.args[0]
        # 判断异常代码, 如果是主题已存在, 则忽略此错误
        if arg.code() != KafkaError.TOPIC_ALREADY_EXISTS:
            raise e

        logger.error("Cannot create topic, reason is: %s", e)


def teardown_module() -> None:
    """
    测试结束
    """
    try:
        # 删除主题
        # admin_client.delete_topics([conf.TOPIC])

        # 关闭消费者
        consumer.close()
    except KafkaException as e:
        logger.error(e)


def test_topic() -> None:
    """
    测试主题操作
    """
    # 列举指定的主题
    # topic 参数为指定的主题, 如果省略则表示所有主题
    topics = admin_client.list_topics(topic=conf.TOPIC, timeout=5)

    # 确认集群 id, 和 cluster.id 配置一致
    assert topics.cluster_id == "BqeKWXR-QCGwzec3IpAmFQ"  # cspell: disable-line

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


TEST_KEY = b"test"
TEST_VALUE = b"Hello Kafka!"


def poll_and_assert() -> None:
    """
    从消费者读取消息并对消息进行断言
    """
    # 消息对象
    msg: Message = None

    # 持续读取消息, 直到读到一条消息
    while msg is None:
        # 读取消息, 超时时间 2s
        msg = consumer.poll(timeout=2.0)

    # 提交偏移量 (同步方式)
    consumer.commit(asynchronous=False)

    # 确认获取的消息无错误
    assert msg.error() is None

    # 确认获取消息的主题和内容
    assert msg.topic() == conf.TOPIC
    assert msg.key() == TEST_KEY
    assert msg.value() == TEST_VALUE


def test_producer_produce_sync() -> None:
    """
    测试通过 producer 同步发送消息

    通过 `producer.produce` 方法可以发送消息, 该方法本身是异步的, 且无返回值,
    所以如果要确认消息发送成功, 则需要通过 `producer.flush` 方法执行一次刷新

    `producer.flush` 方法会将缓冲区的消息全部发送, 并在消息发送成功后返回
    """
    # 异步发送消息
    producer.produce(
        conf.TOPIC,
        key=TEST_KEY,
        value=TEST_VALUE,
    )

    # 同步等待所有的消息发送完毕
    producer.flush()

    # 确认可以读取到消息
    poll_and_assert()


def test_producer_produce_async() -> None:
    """
    测试通过 producer 异步发送消息

    通过 `producer.produce` 方法的 `callback` 参数可以设置一个回调,
    当消息成功发送后该回调会被调用
    """
    # 回调函数是否被调用
    sent = False

    def produce_callback(err: KafkaError, msg: Message) -> None:
        """
        生产者发送消息回调函数

        当生产者成功发送消息后, 该函数被回调 (异步)

        Args:
            err (KafkaError): 发送过程中产生的错误, 如果为 `None` 表示发送成功
            msg (Message): 被发送的消息对象
        """
        # 确认发送成功
        assert err is None

        # 确认发送的消息内容 (主题, Key, Value)
        assert msg.topic() == conf.TOPIC
        assert msg.key() == TEST_KEY
        assert msg.value() == TEST_VALUE

        # 确认发送消息的偏移量
        assert msg.offset() > 0

        nonlocal sent
        sent = True

    # 发送一条消息 (异步)
    producer.produce(
        conf.TOPIC,
        key=TEST_KEY,
        value=TEST_VALUE,
        callback=produce_callback,
    )

    # 轮询生产者注册的回调是否被调用
    while producer.poll(timeout=1.0) == 0:
        pass

    # 确认 produce_callback 函数被调用
    assert sent is True

    # 确认可以读取到消息
    poll_and_assert()


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
