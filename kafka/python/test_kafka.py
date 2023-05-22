import logging
import socket

import conf
from confluent_kafka import KafkaError, Message
from confluent_kafka.admin import AdminClient
from misc import (MessageGenerator, close_consumer, create_consumer,
                  create_producer, create_topic_if_not_exists, poll_and_assert)
from misc.message import MessageData

# 可配置项参考: https://github.com/confluentinc/librdkafka/blob/master/CONFIGURATION.md

# 日志对象
logger = logging.getLogger(__name__)


def test_topic() -> None:
    """
    测试主题操作
    """
    topic_name = "test-topic"

    # 创建主题
    create_topic_if_not_exists(topic_name)

    # 创建管理客户端
    admin_client = AdminClient({
        "bootstrap.servers": conf.BOOTSTRAP_SERVERS,  # 指定 Broker 地址
    })

    # 列举指定的主题 (如果 topic 参数为 None, 则列举所有主题)
    # 返回的结果为 ClusterMetadata 对象, 记录集群元数据, 其中的 topics 属性为主题字典
    meta = admin_client.list_topics(topic=topic_name, timeout=5)

    # 确认集群 ID
    # 该属性和 Broker 上 server.properties 配置文件的 cluster.id 配置项一致
    assert meta.cluster_id == "BqeKWXR-QCGwzec3IpAmFQ"  # cspell: disable-line

    # 确认该主题对应的 Broker 数量, 本例共启动 3 个 Broker 节点
    # 注意, meta.brokers 不是一个数组, 而是 Key 为 Broker ID 的字典
    assert len(meta.brokers) == 3

    # 确认 3 个 Broker 节点的情况
    # 通过 Broker ID 获取 Broker 对象, 确认其主机名和端口号
    b1 = meta.brokers[1]
    assert b1.host == "localhost" and b1.port == 19092

    b2 = meta.brokers[2]
    assert b2.host == "localhost" and b2.port == 19093

    b3 = meta.brokers[3]
    assert b3.host == "localhost" and b3.port == 19094

    # 获取结果中的 Topic 总数, 为 1, 表示本次获取到 1 个主题
    assert len(meta.topics) == 1
    assert topic_name in meta.topics

    # 根据主题名称获取主题对象
    topic = meta.topics[topic_name]
    assert topic.topic == topic_name

    # 确认该主题具有 3 个分区 (分区 id 分别为 0, 1, 2)
    assert len(topic.partitions) == 3

    # 确认 3 个分区的情况
    # 根据分区 ID 获取分区对象, 确认其分区 ID, 副本数, 分区 Leader 和 ISR 分区
    p0 = topic.partitions[0]
    assert p0.id == 0
    assert len(p0.replicas) == 2
    assert p0.leader in p0.replicas
    assert p0.isrs == p0.replicas

    p1 = topic.partitions[1]
    assert p1.id == 1
    assert p1.isrs == p1.replicas
    assert p1.leader in p1.replicas

    p2 = topic.partitions[2]
    assert p2.id == 2
    assert p2.isrs == p2.replicas
    assert p2.leader in p2.replicas


# 实例化消息生成器对象
msg_generator = MessageGenerator()


def test_producer_produce_sync() -> None:
    """
    测试通过 producer 同步发送消息

    通过 `producer.produce` 方法可以发送消息, 该方法本身是异步的, 且无返回值,
    所以如果要确认消息发送成功, 则需要通过 `producer.flush` 方法执行一次刷新

    `producer.flush` 方法会将缓冲区的消息全部发送, 并在消息发送成功后返回
    """
    topic_name = "test-topic"
    group_id = "test-group-1"

    # 创建主题
    create_topic_if_not_exists(topic_name)

    # 创建消费者对象, 注意, 该对象要在测试一开始创建, 以便拿到发送数据前的 offset
    consumer = create_consumer(topic_name, group_id)

    # 创建一条消息
    msg_data = msg_generator.generate()

    try:
        # 创建生产者
        producer = create_producer()

        # 异步发送消息
        producer.produce(
            topic_name,
            key=msg_data.key,
            value=msg_data.value,
        )

        # 同步等待所有的消息发送完毕
        producer.flush()

        # 确认可以读取到消息
        poll_and_assert(consumer, expected_messages=[msg_data])
    finally:
        close_consumer(consumer)


def test_producer_produce_async() -> None:
    """
    测试通过 producer 异步发送消息

    通过 `producer.produce` 方法的 `callback` 参数可以设置一个回调,
    当消息成功发送后该回调会被调用
    """
    topic_name = "test-topic"
    group_id = "test-group-1"

    # 创建主题
    create_topic_if_not_exists(topic_name)

    # 回调函数是否被调用
    sent = False

    # 创建一条消息
    msg_data = msg_generator.generate()

    def produce_callback(err: KafkaError, msg: Message) -> None:
        """
        生产者发送消息回调函数

        当生产者成功发送消息后, 该函数被回调 (异步)

        Args:
        - `err` (`KafkaError`): 发送过程中产生的错误, 如果为 `None` 表示发送成功
        - `msg` (`Message`): 被发送的消息对象
        """
        # 确认发送成功
        assert err is None

        # 确认发送的消息内容 (主题, Key, Value)
        assert msg.topic() == topic_name
        assert msg.key() == msg_data.key
        assert msg.value() == msg_data.value

        # 确认发送消息的偏移量
        assert msg.offset() >= 0

        nonlocal sent
        sent = True

    # 创建消费者对象, 注意, 该对象要在测试一开始创建, 以便拿到发送数据前的 offset
    consumer = create_consumer(topic_name, group_id)

    try:
        # 创建生产者
        producer = create_producer()

        # 发送一条消息 (异步)
        producer.produce(
            topic_name,
            key=msg_data.key,
            value=msg_data.value,
            callback=produce_callback,
        )

        # 轮询生产者注册的回调是否被调用
        while producer.poll(timeout=1.0) == 0:
            pass

        # 确认 produce_callback 函数被调用
        assert sent is True

        # 确认可以读取到消息
        poll_and_assert(consumer, expected_messages=[msg_data])
    finally:
        close_consumer(consumer)


def test_transaction_only_producer() -> None:
    """
    测试事务

    Kafka 事务可以保证生产者发送的消息, 无论写入多少个分区, 都能在事务提交后同时生效,
    在事务取消 (回滚) 后同时失效

    通过事务可以完成 "consume-transform-produce" 模式, 即 "消费-转化-再生产".
    这种模式可以从一个"输入主题"读取消息, 对消息进行加工后, 写入到输出主题, 供另一个消费者消费

    几个要点包括:

    1. 启动事务的生产者要设置 `transactional.id` 配置项, 指定事务的 ID.
    如果多个生产者使用同一个 `transactional.id`, 则先加入集群的生产者会抛出异常;
    所以, 当一个生产者出现故障离开集群, 则新开启的具备相同 `transactional.id`
    的生产者将完全将其取代, 之前的事务会提交或取消;
    2. 生产者设置 `transactional.id` 后, `enable.idempotence`, `retries` 以及
    `acks` 这几个设置项会自动设置为 `true`, `max` 以及 `all`, 分别表示: 开启"精确一致性",
    无限重试以及需要所有副本的响应, 这是为了保障一条消息一定会被成功写入,
    所以设置了 `transactional.id` 后, 可以不设置这几个设置项, 但如果设置错误, 则会抛出异常;
    3. 消费者和事务的关系不大, 并不会因为有了事务就能保证消费者读取所有消息, 但消费者的
    `isolation.level` 配置项可以指定事务的隔离级别, 包括: `read_committed` 和
    `read_uncommitted`, 前者表示事务提交前, 消费者无法消费事务中发送的消息;
    4. 在事务中不能由消费者提交偏移量, 因为这种方式并不能将偏移量提交给所有节点,
    而必须通过生产者的 `send_offsets_to_transaction` 方法将消费偏移量提交给事务控制器
    """
    input_topic_name = "test-topic-in"
    output_topic_name = "test-topic-out"
    group_id = "test-group-1"

    # 创建输入数据主题, 即 input 主题
    create_topic_if_not_exists(input_topic_name)

    # 创建输出数据主题, 即 output 主题
    create_topic_if_not_exists(output_topic_name)

    # 创建向 input 主题写入消息的生产者
    input_producer = create_producer()

    # 创建一条消息
    msg_data = msg_generator.generate()

    # 将消息发送到 input 主题
    input_producer.produce(
        input_topic_name,
        key=msg_data.key,
        value=msg_data.value,
    )

    # 创建从 input 主题读取数据的消费者
    input_consumer = create_consumer(input_topic_name, group_id)

    # 创建向 output 主题写入消息的生产者
    output_producer = create_producer(ext_props={
        "transactional.id": socket.gethostname(),  # 设置事务 ID
        # "enable.idempotence": True,  # 开启精确一次性, 使用事务时必须设置
        # "acks": "all",  # 要求所有分区副本回执, 使用事务时必须设置
    })

    # 在 output 生产者上启动事务
    output_producer.init_transactions()

    try:
        # 在 output 生产者上启动事务
        output_producer.begin_transaction()

        # 从 input 主题中读取最新的一条消息
        # 注意, 这里不能提交偏移量 (包括自动和手动), 所以 commit 参数为 False
        msg = poll_and_assert(
            input_consumer, expected_messages=[msg_data], commit=False,
        )[0]

        def delivery_report(err: KafkaError, msg: Message) -> None:
            """
            生产者分发消息完成后调用的回调函数

            Args:
            - `err` (`KafkaError`): 消息发送失败的错误信息对象
            - `msg` (`Message`): 被发送的消息对象
            """
            if err:
                logger.error(
                    f"Message delivery failed ({msg.topic()} [{msg.partition()}]): {err}")

        # 将消息进行处理后, 发送到 output 主题
        output_producer.produce(
            output_topic_name,
            key=f"{msg.key()}-transformed".encode(),
            value=f"{msg.value()}-transformed".encode(),
            on_delivery=delivery_report,  # 设置当消息分发完成后进行的回调
        )

        # 提交 input 消费者的偏移量, 该偏移量将作为一条"控制"消息包含在整个事务内,
        # 所以当事务取消 (回滚) 后, 该偏移量也作废
        output_producer.send_offsets_to_transaction(
            input_consumer.position(input_consumer.assignment()),
            input_consumer.consumer_group_metadata(),
        )

        # 提交事务
        output_producer.commit_transaction()
    except Exception as e:
        # 取消 (回滚) 事务
        output_producer.abort_transaction()
        raise e
    finally:
        close_consumer(input_consumer)

    # 创建 output 消费者对象, 用于消费 output 主题的消息
    consumer_out = create_consumer(
        output_topic_name,
        group_id,
        ext_props={
            "isolation.level": "read_committed",  # 设置事务隔离级别
        })

    try:
        new_msg_data = MessageData(
            f"{msg_data.key}-transformed".encode(),
            f"{msg_data.value}-transformed".encode(),
        )
        # 读取 output 主题的最新消息并和发送消息做对比
        poll_and_assert(consumer_out, expected_messages=[new_msg_data])
    finally:
        close_consumer(consumer_out)


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
