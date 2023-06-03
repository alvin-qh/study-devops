package alvin.study.kafka;

import static org.assertj.core.api.BDDAssertions.then;

import java.net.InetAddress;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRebalanceListener;
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
            var records = KafkaUtil.pollLastNRecords(consumer, 1, true);

            // 确认获取的消息和发送的消息一致
            then(records).hasSize(1);
            then(records.get(0).topic()).isEqualTo(topicName);
            then(records.get(0).key()).isEqualTo(record.key());
            then(records.get(0).value()).isEqualTo(record.value());

            then(records.get(0).partition()).isIn(0, 1, 2);
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
            var records = KafkaUtil.pollLastNRecords(consumer, 1, true);

            // 确认获取的消息和发送的消息一致
            then(records).hasSize(1);
            then(records.get(0).topic()).isEqualTo(topicName);
            then(records.get(0).key()).isEqualTo(record.key());
            then(records.get(0).value()).isEqualTo(record.value());

            then(records.get(0).partition()).isIn(0, 1, 2);
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
                then(records).hasSize(1);

                var record = records.get(0);
                then(record.topic()).isEqualTo(inputTopicName);
                then(record.key()).isEqualTo(inputRecord.key());
                then(record.value()).isEqualTo(inputRecord.value());
                then(record.partition()).isIn(0, 1, 2);

                // 将 input 消息处理后, 产生 output 消息
                var outputRecord = new ProducerRecord<>(
                    outputTopicName,
                    record.key() + "_transformed",
                    record.value() + "_transformed");

                // 将 output 消息发送到 output 主题
                outputProducer.send(outputRecord);

                // 提交 input 消费者的偏移量, 该偏移量将作为一条"控制"消息包含在整个事务内, 所以当事务取消 (回滚) 后, 该偏移量也作废
                outputProducer.sendOffsetsToTransaction(
                    KafkaUtil.createPartitionOffsetFromConsumer(inputConsumer),
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
            var records = KafkaUtil.pollLastNRecords(outputConsumer, 1, true);

            // 确认获取的消息和发送的消息一致
            then(records).hasSize(1);

            var record = records.get(0);
            then(record.topic()).isEqualTo(outputTopicName);
            then(record.key()).isEqualTo(inputRecord.key() + "_transformed");
            then(record.value()).isEqualTo(inputRecord.value() + "_transformed");
            then(record.partition()).isIn(0, 1, 2);
        }
    }
}
