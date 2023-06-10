import logging
import os
import socket
from typing import Dict
from uuid import uuid4

import conf
from confluent_kafka import Consumer, KafkaError, Message, TopicPartition
from confluent_kafka.admin import AdminClient
from confluent_kafka.serialization import MessageField, SerializationContext
from misc import (AvroSchema, JsonSchema, MessageData, MessageGenerator,
                  close_consumer, create_consumer, create_producer,
                  create_topic, poll_and_assert)

# 可配置项参考: https://github.com/confluentinc/librdkafka/blob/master/CONFIGURATION.md

# 日志对象
logger = logging.getLogger(__name__)


def test_topic() -> None:
    """
    测试主题操作
    """
    topic_name = "py-test__simple-topic"

    # 创建主题
    create_topic(topic_name)

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
    assert set(p0.isrs) == set(p0.replicas)

    p1 = topic.partitions[1]
    assert p1.id == 1
    assert len(p1.replicas) == 2
    assert p1.isrs == p1.replicas
    assert set(p1.isrs) == set(p1.replicas)

    p2 = topic.partitions[2]
    assert p2.id == 2
    assert len(p2.replicas) == 2
    assert p2.isrs == p2.replicas
    assert set(p2.isrs) == set(p2.replicas)


# 实例化消息生成器对象
msg_generator = MessageGenerator()


def test_producer_produce_sync() -> None:
    """
    测试通过 producer 同步发送消息

    通过 `producer.produce` 方法可以发送消息, 该方法本身是异步的, 且无返回值,
    所以如果要确认消息发送成功, 则需要通过 `producer.flush` 方法执行一次刷新

    `producer.flush` 方法会将缓冲区的消息全部发送, 并在消息发送成功后返回
    """
    topic_name = "py-test__simple-topic"
    group_id = "py-test__simple-group"

    # 创建主题
    create_topic(topic_name)

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
    topic_name = "py-test__simple-topic"
    group_id = "py-test__simple-group"

    # 创建主题
    create_topic(topic_name)

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


def test_transaction() -> None:
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
    input_topic_name = "py-test__ctp-input-topic"
    output_topic_name = "py-test__ctp-output-topic"
    group_id = "py-test__ctp_group"

    # 创建输入数据主题, 即 input 主题
    create_topic(input_topic_name)

    # 创建输出数据主题, 即 output 主题
    create_topic(output_topic_name)

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
        )[-1]

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


def test_seek_consumer_offset() -> None:
    """
    移动消费者偏移量

    移动消费者偏移量可以重新获取到"之前"的消息, 几个注意点:
    1. 创建消费者时, `auto.offset.reset` 配置项要设置为 `earliest`;
    2. 消费者的 `seek` 方法参数为 `TopicPartition` 对象,
    即偏移量的移动是针对指定主题和分区的偏移量;
    3. 偏移量提交并不包含发生移动的偏移量;
    """
    topic_name = "py-test__seek-topic"
    group_id = "py-test__seek-group"

    # 创建主题
    create_topic(topic_name)

    # 创建消费者对象
    consumer = create_consumer(topic_name, group_id)

    # 创建生产者对象
    producer = create_producer()

    # 生成一条消息
    msg_data = msg_generator.generate()

    # 向主题发送一条消息
    producer.produce(
        topic_name,
        key=msg_data.key,
        value=msg_data.value,
    )

    try:
        # 获取最新的消息并进行确认
        msg = poll_and_assert(consumer, expected_messages=[msg_data])[-1]

        # 将消费者中指定主题分区的偏移量设置为前一个消息的偏移量
        consumer.seek(TopicPartition(
            topic=msg.topic(),
            partition=msg.partition(),
            offset=msg.offset(),
        ))

        # 再次读取消息, 确认可以读取到前一个消息
        # 注意, 在前面调用 poll_and_assert 函数, 内部已经提交了偏移量, 当偏移量 seek 后,
        # 则无需再次提交偏移量
        msg = consumer.poll(timeout=2.0)
        assert msg
        assert msg.error() is None

        assert msg.key() == msg_data.key
        assert msg.value() == msg_data.value
    finally:
        close_consumer(consumer)


def test_group_regular_member() -> None:
    """
    群组固定成员

    默认情况下, 群组成员是动态的, 即一个群组成员离开或加入群组, 都会导致消费组再平衡,
    即重新为每个消费者重新分配分区, 这个过程会导致长时间等待

    通过消费者的 `group.instance.id` 配置项可以指定一个消费者在群组中的"固定"标识,
    当一个消费者离开群组, 稍后又以相同的 `group.instance.id` 加入回群组时,
    群组无需进行再平衡, 而会直接把原本该消费者负责的分区重新分配给该消费者

    通过 `session.timeout.ms` 参数可以指定群组的固定成员离开多久后, 群组会进行再平衡,
    将该分区交给其它消费者处理, 另外 `heartbeat.interval.ms` 则是消费者发送心跳包的时间间隔,
    必须小于 `session.timeout.ms` 设置的时间

    该测试将 `session.timeout.ms` 设置为 3 分钟, 所以观察 Kafka 日志, 在 3 分钟内,
    执行该测试, 不会发生消费者离开或加入群组的操作, 同时也不会发生群组再平衡
    """
    topic_name = "py-test__regular-member-topic"
    group_id = "py-test__regular-member-group"

    # 创建主题
    create_topic(topic_name)

    # 创建消费者对象
    consumer = create_consumer(topic_name, group_id, ext_props={
        "group.instance.id": "9d1b90b5-29a2-4fdd-96d8-96fa8d5ae2ee",  # 固定群组 ID
        "session.timeout.ms": 180000,  # 会话超时时间
        "heartbeat.interval.ms": 18000,  # 心跳发送时间间隔
    })

    # 创建生产者对象
    producer = create_producer()

    # 生成一条消息
    msg_data = msg_generator.generate()

    # 向主题发送一条消息
    producer.produce(
        topic_name,
        key=msg_data.key,
        value=msg_data.value,
    )

    try:
        # 获取最新的消息并进行确认
        poll_and_assert(consumer, expected_messages=[msg_data])
    finally:
        close_consumer(consumer)


def test_assign_specified_partition() -> None:
    """
    独立消费者

    所谓独立消费者, 即不直接进行主题订阅, 而是为该消费者直接分配确定的主题和分区, 有如下特点:
    1. 固定消费者直接从指定主题的分区获取消息, 该组不再进行再平衡操作;
    2. 和固定消费者同组的消费者均必须为固定消费者 (不能发生主题订阅), 且各自关联的主题分区不能交叉;
    """
    topic_name = "py-test__assign-topic"
    group_id = "py-test__assign-group"

    # 创建主题
    create_topic(topic_name)

    # 创建消费者对象, 并且不直接进行主题订阅
    consumer = Consumer({
        "bootstrap.servers": conf.BOOTSTRAP_SERVERS,
        "group.id": group_id,
        "enable.auto.commit": False,
        "auto.offset.reset": "earliest",
        "enable.partition.eof": True
    })

    # 为消费者分配指定的主题和分区
    consumer.assign([
        TopicPartition(
            topic=topic_name,
            partition=1,  # 指定接收分区 1 的消息
        ),
        TopicPartition(
            topic=topic_name,
            partition=2,  # 指定接收分区 2 的消息
        ),
    ])

    # 创建生产者对象
    producer = create_producer()

    # 记录消息和分区对应关系的字典
    partition_msg: Dict[int, MessageData] = {}

    # 向三个分区各发送一条消息
    for partition in range(3):
        # 生成一条消息
        msg_data = msg_generator.generate()

        # 发送消息
        producer.produce(
            topic_name,
            key=msg_data.key,
            value=msg_data.value,
            partition=partition,  # 指定发送消息的目标分区
        )

        partition_msg[partition] = msg_data

    try:
        # 读取消息, 并确认只有分区 1 和 2 的消息返回
        poll_and_assert(
            consumer,
            expected_messages=[partition_msg[1], partition_msg[2]],
        )
    finally:
        close_consumer(consumer)


# 保存 Avro Schema 描述的文件路径
avro_schema_file = os.path.realpath(os.path.join(
    os.path.dirname(__file__),
    "../schema/customer.avro"
))


def test_avro_serialize() -> None:
    """
    通过 `Avro` 协议进行消息的序列化和反序列化
    """
    # 创建 AvroSchema 对象
    schema = AvroSchema("http://localhost:18081", schema_file=avro_schema_file)

    # 创建 Avro 序列化器对象
    # 本例中序列化的对象为 Dict 类型, 所以无需传递 to_dict 参数
    serializer = schema.create_serializer()

    # 创建 Avro 反序列器对象
    # 本例中反序列化的结果为 Dict 类型, 所以无需传递 from_dict 参数
    deserializer = schema.create_deserializer()

    topic_name = "py-test__avro-topic"
    group_id = "py-test__avro-group"

    # 尝试创建主题
    create_topic(topic_name)

    # 创建消费者对象
    consumer = create_consumer(topic_name, group_id)

    # 要发送的消息对象
    # 如果是 Dict 类型对象, 则可以直接发送, 如果是其它类型对象, 则需要转换为 Dict 类型对象
    src_obj = {
        "id": 1,
        "name": "Alvin",
        "email": "alvin.qh@fakemail.com",
    }

    # 创建消息 Key 和 Value
    # Key 为字符串, Value 为 Avro 序列化结果
    key, value = (
        f"key-{uuid4()}".encode(),
        serializer(
            src_obj,
            ctx=SerializationContext(topic_name, MessageField.VALUE),
        )
    )

    # 创建生产者对象
    producer = create_producer()

    # 向主题发送一条消息
    producer.produce(
        topic_name,
        key=key,
        value=value,
    )

    try:
        # 获取最新的一条消息
        msg = poll_and_assert(consumer)[-1]

        # 确认消息的主题, Key 和 Value
        assert msg.topic() == topic_name
        assert msg.key() == key
        assert msg.value() == value

        # 对获取的消息 Value 进行反序列化, 确认反序列化结果正确
        dst_obj = deserializer(
            msg.value(),
            SerializationContext(msg.topic(), MessageField.VALUE)
        )
        assert dst_obj == src_obj
    finally:
        close_consumer(consumer)


# 保存 Json Schema 描述的文件路径
json_schema_file = os.path.realpath(os.path.join(
    os.path.dirname(__file__),
    "../schema/customer.json"
))


def test_json_serialize() -> None:
    """
    通过 `Json` 协议进行消息的序列化和反序列化
    """
    # 创建 JsonSchema 对象
    schema = JsonSchema("http://localhost:18081", schema_file=json_schema_file)

    # 创建 JSON 序列化器对象
    # 本例中序列化的对象为 Dict 类型, 所以无需传递 to_dict 参数
    serializer = schema.create_serializer()

    # 创建 JSON 反序列器对象
    # 本例中反序列化的结果为 Dict 类型, 所以无需传递 from_dict 参数
    deserializer = schema.create_deserializer()

    topic_name = "py-test__json-topic"
    group_id = "py-test__json-group"

    # 尝试创建主题
    create_topic(topic_name)

    # 创建消费者对象
    consumer = create_consumer(topic_name, group_id)

    # 要发送的消息对象
    # 如果是 Dict 类型对象, 则可以直接发送, 如果是其它类型对象, 则需要转换为 Dict 类型对象
    src_obj = {
        "id": 1,
        "name": "Alvin",
        "email": "alvin.qh@fakemail.com",
    }

    # 创建消息 Key 和 Value
    # Key 为字符串, Value 为 JSON 序列化结果
    key, value = (
        f"key-{uuid4()}".encode(),
        serializer(
            src_obj,
            ctx=SerializationContext(topic_name, MessageField.VALUE),
        )
    )

    # 创建生产者对象
    producer = create_producer()

    # 向主题发送一条消息
    producer.produce(
        topic_name,
        key=key,
        value=value,
    )

    try:
        # 获取最新的一条消息
        msg = poll_and_assert(consumer)[-1]

        # 确认消息的主题, Key 和 Value
        assert msg.topic() == topic_name
        assert msg.key() == key
        assert msg.value() == value

        # 对获取的消息 Value 进行反序列化, 确认反序列化结果正确
        dst_obj = deserializer(
            msg.value(),
            SerializationContext(msg.topic(), MessageField.VALUE)
        )
        assert dst_obj == src_obj
    finally:
        close_consumer(consumer)
