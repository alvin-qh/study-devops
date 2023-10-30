package alvin.study.kafka;

import static org.assertj.core.api.BDDAssertions.then;

import java.net.InetAddress;
import java.time.Duration;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecord;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRebalanceListener;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.Test;

import alvin.study.kafka.misc.KafkaRecordGenerator;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;

/**
 * 测试 Kafka
 */
class KafkaTest {
    private final KafkaRecordGenerator recordGenerator = new KafkaRecordGenerator();

    /**
     * 测试创建 Kafka 主题
     */
    @Test
    void topic_shouldCreateTopicIfNotExist() throws Exception {
        var topicName = "java-test__simple-topic";

        // 创建指定主题
        KafkaUtil.createTopic(topicName);

        // 获取管理客户端对象
        try (var ac = KafkaClientBuilder.createAdminClient()) {
            // 获取全部主题
            var topics = ac.listTopics();
            then(topics.names().get()).contains(topicName);
        }
    }

    /**
     * 消费者组再平衡监听器
     */
    @RequiredArgsConstructor
    static class RebalanceListener implements ConsumerRebalanceListener {
        private final String topic;

        /**
         * 当发生再平衡且消费者分配到指定主题的新分区时被调用, 传入新的分区列表
         *
         * @param partitions 分区列表
         */
        @Override
        public void onPartitionsAssigned(Collection<TopicPartition> partitions) {
            for (var partition : partitions) {
                then(partition.topic()).isEqualTo(topic);
                then(partition.partition()).isIn(0, 1, 2);
            }
        }

        /**
         * 当发生再平衡且消费者放弃指定主题的分区时被调用, 传入被放弃的分区列表
         *
         * @param partitions 分区列表
         */
        @Override
        public void onPartitionsRevoked(Collection<TopicPartition> partitions) {
            for (var partition : partitions) {
                then(partition.topic()).isEqualTo(topic);
                then(partition.partition()).isIn(0, 1, 2);
            }
        }

        /**
         * 当发生意外的再平衡并且参数集合中的分区已经有新的所有者的情况下被调用, 传入被放弃的分区列表
         *
         * @param partitions 分区列表
         */
        @Override
        public void onPartitionsLost(Collection<TopicPartition> partitions) {
            onPartitionsRevoked(partitions);
        }
    }

    /**
     * 测试发送 (同步方式) 和接收消息
     */
    @Test
    @SneakyThrows
    void produce_shouldProducerProduced() {
        var topicName = "java-test__simple-topic";
        var groupId = "java-test__simple-group";

        // 创建指定主题
        KafkaUtil.createTopic(topicName);

        // 生成一条消息
        var record = recordGenerator.generate(topicName);

        // 创建生产者
        try (var producer = KafkaClientBuilder.<String, String>createProducer()) {
            // 发送消息
            var future = producer.send(record);

            // 获取消息发送结果
            var meta = future.get(5, TimeUnit.SECONDS);

            // 确认消息发送结果
            then(meta.topic()).isEqualTo(topicName);
            then(meta.hasOffset()).isTrue();
            then(meta.offset()).isGreaterThanOrEqualTo(0L);
            then(meta.partition()).isIn(0, 1, 2);
        }

        // 创建消费者
        try (var consumer = KafkaClientBuilder.<String, String>createConsumer(
            topicName, groupId, new RebalanceListener(topicName))) {
            // 获取最新的 1 条消息
            var records = KafkaUtil.pollLastNRecords(consumer, 1);

            // 确认获取的消息和发送的消息一致
            then(records.count()).isEqualTo(1);

            var r = KafkaUtil.firstResult(records).get();
            then(r.topic()).isEqualTo(topicName);
            then(r.key()).isEqualTo(record.key());
            then(r.value()).isEqualTo(record.value());
            then(r.partition()).isIn(0, 1, 2);
        }
    }

    /**
     * 测试发送 (异步方式) 和接收消息
     */
    @Test
    @SneakyThrows
    void produce_shouldProducerProducedAsync() {
        var topicName = "java-test__simple-topic";
        var groupId = "java-test__simple-group";

        // 创建指定主题
        KafkaUtil.createTopic(topicName);

        // 生成一条消息
        var record = recordGenerator.generate(topicName);

        // 用于等待生产结束的计数器
        var countDown = new CountDownLatch(1);

        // 创建生产者
        try (var producer = KafkaClientBuilder.<String, String>createProducer()) {
            // 发送消息
            producer.send(record, (meta, e) -> {
                // 确认消息发送结果
                then(meta.topic()).isEqualTo(topicName);
                then(meta.hasOffset()).isTrue();
                then(meta.offset()).isGreaterThanOrEqualTo(0L);
                then(meta.partition()).isIn(0, 1, 2);

                // 增加计数器
                countDown.countDown();
            });
        }

        // 等待计数器到达指定值
        then(countDown.await(10, TimeUnit.SECONDS)).isTrue();

        // 创建消费者
        try (var consumer = KafkaClientBuilder.<String, String>createConsumer(topicName, groupId)) {
            // 获取最新的 1 条消息
            var records = KafkaUtil.pollLastNRecords(consumer, 1);

            // 确认获取的消息和发送的消息一致
            then(records.count()).isEqualTo(1);

            var r = KafkaUtil.firstResult(records).get();
            then(r.topic()).isEqualTo(topicName);
            then(r.key()).isEqualTo(record.key());
            then(r.value()).isEqualTo(record.value());
            then(r.partition()).isIn(0, 1, 2);
        }
    }

    /**
     * 测试通过事务进行消息发送和偏移量提交
     *
     * <p>
     * Kafka 事务可以保证生产者发送的消息, 无论写入多少个分区, 都能在事务提交后同时生效, 在事务取消 (回滚) 后同时失效
     * </p>
     *
     * <p>
     * 通过事务可以完成 "consume-transform-produce" 模式, 即 "消费-转化-再生产". 这种模式可以从一个"输入主题"读取消息,
     * 对消息进行加工后, 写入到输出主题, 供另一个消费者消费
     * </p>
     *
     * <p>
     * 几个要点包括:
     * <ol>
     * <li>
     * 启动事务的生产者要设置 {@code transactional.id} 配置项, 指定事务的 ID. 如果多个生产者使用同一个 {@code transactional.id},
     * 则先加入集群的生产者会抛出异常; 所以, 当一个生产者出现故障离开集群, 则新开启的具备相同 {@code transactional.id} 的生产者将完全将其取代,
     * 之前的事务会提交或取消;
     * </li>
     * <li>
     * 生产者设置 {@code transactional.id} 后, {@code enable.idempotence}, {@code retries} 以及 {@code acks} 这几个设置项会自动设置为
     * {@code true}, {@code max} 以及 {@code all}, 分别表示: 开启"精确一致性", "无限重试"以及"需要所有副本的响应",
     * 这是为了保障一条消息一定会被成功写入, 所以设置了 {@code transactional.id} 后, 可以不设置这几个设置项, 但如果设置错误, 则会抛出异常;
     * </li>
     * <li>
     * 消费者和事务的关系不大, 并不会因为有了事务就能保证消费者读取所有消息, 但消费者的 {@code isolation.level} 配置项可以指定事务的隔离级别,
     * 包括: {@code read_committed} 和 {@code read_uncommitted}, 前者表示事务提交前, 消费者无法消费事务中发送的消息;
     * </li>
     * <li>
     * 在事务中不能由消费者提交偏移量, 因为这种方式并不能将偏移量提交给所有节点, 而必须通过生产者的 {@code send_offsets_to_transaction}
     * 方法将消费偏移量提交给事务控制器
     * </li>
     * </ol>
     * </p>
     */
    @Test
    @SneakyThrows
    void transactional_shouldSendMessageWithTransactional() {
        var inputTopicName = "java-test__ctp-input-topic";
        var outputTopicName = "java-test__ctp-output-topic";
        var groupId = "java-test__ctp_group";

        // 创建输入和输出的主题
        KafkaUtil.createTopic(inputTopicName);
        KafkaUtil.createTopic(outputTopicName);

        // 生成一条消息
        var inputRecord = recordGenerator.generate(inputTopicName);

        try (var inputProducer = KafkaClientBuilder.<String, String>createProducer()) {
            // 发送消息
            var future = inputProducer.send(inputRecord);

            // 获取消息发送结果
            var meta = future.get(5, TimeUnit.SECONDS);

            // 确认消息发送结果
            then(meta.topic()).isEqualTo(inputTopicName);
            then(meta.hasOffset()).isTrue();
            then(meta.offset()).isGreaterThanOrEqualTo(0L);
            then(meta.partition()).isIn(0, 1, 2);
        }

        // 创建 output 生产者, 用于将消息发送到 output 主题
        // 由于要使用事务, 所以需要为 output 消费者设置 "transactional.id" 配置项
        try (var outputProducer = KafkaClientBuilder.<String, String>createProducer(Map.of(
            ProducerConfig.TRANSACTIONAL_ID_CONFIG, InetAddress.getLocalHost().getHostName()))) {
            // 初始化事务
            outputProducer.initTransactions();

            // 启动事务
            outputProducer.beginTransaction();

            try (var inputConsumer = KafkaClientBuilder.<String, String>createConsumer(inputTopicName, groupId)) {
                // 从 input 主题中读取最新的 1 条消息, 注意, 事务中的消费者不能通过自身提交消息偏移量
                var records = KafkaUtil.pollLastNRecords(inputConsumer, 1, false);

                // 确认获取的消息和发送的消息一致
                then(records.count()).isEqualTo(1);

                var r = KafkaUtil.firstResult(records).get();
                then(r.topic()).isEqualTo(inputTopicName);
                then(r.key()).isEqualTo(inputRecord.key());
                then(r.value()).isEqualTo(inputRecord.value());
                then(r.partition()).isIn(0, 1, 2);

                // 将 input 消息处理后, 产生 output 消息
                var outputRecord = new ProducerRecord<>(
                    outputTopicName,
                    r.key() + "_transformed",
                    r.value() + "_transformed");

                // 将 output 消息发送到 output 主题
                outputProducer.send(outputRecord);

                // 提交 input 消费者的偏移量, 该偏移量将作为一条"控制"消息包含在整个事务内, 所以当事务取消 (回滚) 后, 该偏移量也作废
                outputProducer.sendOffsetsToTransaction(
                    KafkaUtil.partitionOffsetFromConsumer(inputConsumer),
                    inputConsumer.groupMetadata());

                // 正常完成, 提交事务
                outputProducer.commitTransaction();
            } catch (Exception e) {
                // 出现问题, 取消 (回滚) 事务
                outputProducer.abortTransaction();
            }
        }

        // 创建消费者, 从 output 分区读取消息, 这里只读取上一个事务已提交的消息, 所以需要将消费者的 "isolation.level"
        // 配置项设置为 "read_committed"
        try (var outputConsumer = KafkaClientBuilder.<String, String>createConsumer(
            outputTopicName, groupId, Map.of(ConsumerConfig.ISOLATION_LEVEL_CONFIG, "read_committed"))) {
            // 从 output 主题中读取最新的 1 条消息
            var records = KafkaUtil.pollLastNRecords(outputConsumer, 1);

            // 确认获取的消息和发送的消息一致
            then(records.count()).isEqualTo(1);

            var r = KafkaUtil.firstResult(records).get();
            then(r.topic()).isEqualTo(outputTopicName);
            then(r.key()).isEqualTo(inputRecord.key() + "_transformed");
            then(r.value()).isEqualTo(inputRecord.value() + "_transformed");
            then(r.partition()).isIn(0, 1, 2);
        }
    }

    /**
     * 移动消费者偏移量
     *
     * <p>
     * 移动消费者偏移量可以重新获取到"之前"的消息, 几个注意点:
     * <ol>
     * <li>
     * 创建消费者时, {@code auto.offset.reset} 配置项要设置为 {@code earliest};
     * </li>
     * <li>
     * 消费者的 {@link org.apache.kafka.clients.consumer.Consumer#seek(TopicPartition, long)
     * Consumer.seek(TopicPartition, long)} 方法参数为 {@link TopicPartition} 对象, 即偏移量的移动是针对指定主题和分区的偏移量;
     * </li>
     * <li>
     * 偏移量提交并不包含发生移动的偏移量;
     * </li>
     * </ol>
     * </p>
     */
    @Test
    @SneakyThrows
    void seek_shouldSeekConsumerOffset() {
        var topicName = "java-test__seek-topic";
        var groupId = "java-test__seek-group";

        // 创建主题
        KafkaUtil.createTopic(topicName);

        // 生成一条消息
        var record = recordGenerator.generate(topicName);

        // 创建生产者
        try (var producer = KafkaClientBuilder.<String, String>createProducer()) {
            // 发送消息
            var future = producer.send(record);

            // 获取发送结果
            var meta = future.get(5, TimeUnit.SECONDS);

            // 确认消息发送成功
            then(meta.topic()).isEqualTo(topicName);
            then(meta.hasOffset()).isTrue();
            then(meta.offset()).isGreaterThanOrEqualTo(0L);
            then(meta.partition()).isIn(0, 1, 2);
        }

        // 创建消费者
        try (var consumer = KafkaClientBuilder.<String, String>createConsumer(topicName, groupId)) {
            // 接收最新的 1 条消息
            var records = KafkaUtil.pollLastNRecords(consumer, 1);

            // 确认消息接收正确
            then(records.count()).isEqualTo(1);

            var r = KafkaUtil.firstResult(records).get();
            then(r.topic()).isEqualTo(topicName);
            then(r.key()).isEqualTo(record.key());
            then(r.value()).isEqualTo(record.value());
            then(r.partition()).isIn(0, 1, 2);

            // 将消费者偏移量移动到上一条消息的位置
            consumer.seek(new TopicPartition(r.topic(), r.partition()), r.offset());

            // 再次获取消息, 注意, 本次无需再次提交偏移量
            records = consumer.poll(Duration.ofSeconds(2));

            // 确认可以获取到上次获取到的消息
            then(records.count()).isEqualTo(1);

            r = KafkaUtil.firstResult(records).get();
            then(r.topic()).isEqualTo(topicName);
            then(r.key()).isEqualTo(record.key());
            then(r.value()).isEqualTo(record.value());
            then(r.partition()).isIn(0, 1, 2);
        }
    }

    /**
     * 群组固定成员
     *
     * <p>
     * 默认情况下, 群组成员是动态的, 即一个群组成员离开或加入群组, 都会导致消费组再平衡, 即重新为每个消费者重新分配分区,
     * 这个过程会导致长时间等待
     * </p>
     *
     * <p>
     * 通过消费者的 {@code group.instance.id} 配置项可以指定一个消费者在群组中的"固定"标识, 当一个消费者离开群组,
     * 稍后又以相同的 {@code group.instance.id} 加入回群组时, 群组无需进行再平衡, 而会直接把原本该消费者负责的分区重新分配给该消费者
     * </p>
     *
     * <p>
     * 通过 {@code session.timeout.ms} 参数可以指定群组的固定成员离开多久后, 群组会进行再平衡, 将该分区交给其它消费者处理, 另外
     * {@code heartbeat.interval.ms} 则是消费者发送心跳包的时间间隔, 必须小于 {@code session.timeout.ms} 设置的时间
     * </p>
     *
     * 该测试将 {@code session.timeout.ms} 设置为 3 分钟, 所以观察 Kafka 日志, 在 3 分钟内, 执行该测试,
     * 不会发生消费者离开或加入群组的操作, 同时也不会发生群组再平衡
     * </p>
     */
    @Test
    @SneakyThrows
    void consumer_shouldBecameGroupRegularMember() {
        var topicName = "java-test__regular-member-topic";
        var groupId = "java-test__regular-member-group";

        // 创建主题
        KafkaUtil.createTopic(topicName);

        // 创建一条消息
        var record = recordGenerator.generate(topicName);

        // 创建生产者
        try (var producer = KafkaClientBuilder.<String, String>createProducer()) {
            // 发送消息
            var future = producer.send(record);

            // 获取发送结果
            var meta = future.get(2, TimeUnit.SECONDS);

            // 确认消息发送成功
            then(meta.topic()).isEqualTo(topicName);
            then(meta.hasOffset()).isTrue();
            then(meta.offset()).isGreaterThanOrEqualTo(0L);
            then(meta.partition()).isIn(0, 1, 2);
        }

        // 创建消费者, 设置 "group.instance.id" 配置项作为固定消费组成员 ID,
        // 并设置 "session.timeout.ms" 和 "heartbeat.interval.ms" 两个时间配置项
        try (var consumer = KafkaClientBuilder.<String, String>createConsumer(topicName, groupId, Map.of(
            ConsumerConfig.GROUP_INSTANCE_ID_CONFIG, "9d1b90b5-29a2-4fdd-96d8-96fa8d5ae2ee", // 固定群组 ID
            ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, "180000", // 会话超时时间
            ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG, "18000" // 心跳发送时间间隔
        ))) {
            // 接收最新的 1 条消息
            var records = KafkaUtil.pollLastNRecords(consumer, 1);

            // 确认消息接收正确
            then(records.count()).isEqualTo(1);

            var r = KafkaUtil.firstResult(records).get();
            then(r.topic()).isEqualTo(topicName);
            then(r.key()).isEqualTo(record.key());
            then(r.value()).isEqualTo(record.value());
            then(r.partition()).isIn(0, 1, 2);
        }
    }

    /**
     * 独立消费者
     *
     * <p>
     * 所谓独立消费者, 即不直接进行主题订阅, 而是为该消费者直接分配确定的主题和分区, 有如下特点:
     * <ol>
     * <li>
     * 固定消费者直接从指定主题的分区获取消息, 该组不再进行再平衡操作;
     * </li>
     * <li>
     * 和固定消费者同组的消费者均必须为固定消费者 (不能发生主题订阅), 且各自关联的主题分区不能交叉;
     * </li>
     * </ol>
     * </p>
     */
    @Test
    @SneakyThrows
    void consumer_shouldAssignedSpecifiedPartition() {
        var topicName = "java-test__assign-topic";
        var groupId = "java-test__assign-group";

        // 创建主题
        KafkaUtil.createTopic(topicName);

        // 记录已发送消息和分区关系的 Map
        var partitionRecord = new HashMap<Integer, ProducerRecord<String, String>>();

        // 创建生产者
        try (var producer = KafkaClientBuilder.<String, String>createProducer()) {
            // 向每个分区各发送一条消息
            for (var i = 0; i < 3; i++) {
                var record = recordGenerator.generate(topicName, i);

                // 发送消息
                var future = producer.send(record);

                // 获取消息发送结果
                var meta = future.get(2, TimeUnit.SECONDS);
                then(meta.topic()).isEqualTo(topicName);
                then(meta.hasOffset()).isTrue();
                then(meta.offset()).isGreaterThanOrEqualTo(0L);
                then(meta.partition()).isIn(0, 1, 2);

                // 记录分区和已发送消息的关系
                partitionRecord.put(i, record);
            }
        }

        // 创建消费者, 由于要分配固定主题分区, 所以这里不对主题进行订阅
        try (var consumer = new KafkaConsumer<String, String>(KafkaClientConfig.loadConsumerConfig(Map.of(
            ConsumerConfig.GROUP_ID_CONFIG, groupId // 设置消费组
        )))) {
            // 为消费者分配主题和分区
            consumer.assign(List.of(
                new TopicPartition(topicName, 1), // 分配主题的分区 1
                new TopicPartition(topicName, 2)  // 分配主题的分区 2
            ));

            // 接收最新的 3 条消息
            var records = KafkaUtil.pollLastNRecords(consumer, 3);

            // 确认消息接收正确
            then(records.count()).isEqualTo(2);

            // 确认接收的消息
            for (var r : records) {
                // 确认没有 0 分区的消息
                then(r.partition()).isNotEqualTo(0);

                // 确认接收消息正确
                var record = partitionRecord.get(r.partition());
                then(r.topic()).isEqualTo(topicName);
                then(r.key()).isEqualTo(record.key());
                then(r.value()).isEqualTo(record.value());
            }
        }
    }

    /**
     * 通过 Avro 协议进行消息的序列化和反序列化
     *
     * <p>
     * 所谓独立消费者, 即不直接进行主题订阅, 而是为该消费者直接分配确定的主题和分区, 有如下特点:
     * <ol>
     * <li>
     * 固定消费者直接从指定主题的分区获取消息, 该组不再进行再平衡操作;
     * </li>
     * <li>
     * 和固定消费者同组的消费者均必须为固定消费者 (不能发生主题订阅), 且各自关联的主题分区不能交叉;
     * </li>
     * </ol>
     * </p>
     */
    @Test
    @SneakyThrows
    void avro_shouldUseRecordWithAvroSchema() {
        var topicName = "java-test__avro-topic";
        var groupId = "java-test__avro-group";

        // 创建主题
        KafkaUtil.createTopic(topicName);

        // 记录发送的消息对象
        ProducerRecord<String, GenericRecord> record = null;

        // 创建生产者
        // 对于消息的 Value, 设置 value.serializer 配置项以支持 Avro 序列化
        // 指定 schema.registry.url 配置项, 设置 Schema Registry 服务地址
        try (var producer = KafkaClientBuilder.<String, GenericRecord>createProducer(Map.of(
            ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, "io.confluent.kafka.serializers.KafkaAvroSerializer",
            "schema.registry.url", "http://localhost:18081" // 设置 Schema Registry 服务地址
        ))) {
            // 通过 Schema 定义, 创建 Avro 数据对象
            var value = new GenericData.Record(KafkaUtil.parseSchema("../schema/customer.avro"));
            // 设置数据字段
            value.put("id", 1);
            value.put("name", "Alvin");
            value.put("email", "alvin@fake-mail.com");

            // 生成要发送的消息对象
            record = new ProducerRecord<>(topicName, "key-" + UUID.randomUUID(), value);

            // 发送消息
            var future = producer.send(record);

            // 获取消息发送结果
            var meta = future.get(2, TimeUnit.SECONDS);
            then(meta.topic()).isEqualTo(topicName);
            then(meta.hasOffset()).isTrue();
            then(meta.offset()).isGreaterThanOrEqualTo(0L);
            then(meta.partition()).isIn(0, 1, 2);
        }

        // 创建消费者
        // 对于消息的 Value, 设置 value.deserializer 配置项以支持 Avro 反序列化
        // 指定 schema.registry.url 配置项, 设置 Schema Registry 服务地址
        try (var consumer = KafkaClientBuilder.<String, GenericRecord>createConsumer(topicName, groupId, Map.of(
            ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, "io.confluent.kafka.serializers.KafkaAvroDeserializer",
            "schema.registry.url", "http://localhost:18081" // 设置 Schema Registry 服务地址
        ))) {
            // 读取最新的 1 条消息
            var records = KafkaUtil.pollLastNRecords(consumer, 1);

            // 确认读取的消息和发送的消息一致
            var r = KafkaUtil.firstResult(records).get();
            then(r.topic()).isEqualTo(topicName);
            then(r.key()).isEqualTo(record.key());
            then(r.value().get("id"))
                    .isEqualTo(record.value().get("id"))
                    .isEqualTo(1);
            then(r.value().get("name"))
                    .hasToString(record.value().get("name").toString())
                    .hasToString("Alvin");
            then(r.value().get("email"))
                    .hasToString(record.value().get("email").toString())
                    .hasToString("alvin@fake-mail.com");
        }
    }
}
