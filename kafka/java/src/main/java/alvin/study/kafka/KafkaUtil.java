package alvin.study.kafka;

import java.io.File;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.apache.avro.Schema;
import org.apache.kafka.clients.admin.CreateTopicsOptions;
import org.apache.kafka.clients.admin.CreateTopicsResult;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;

/**
 * Kafka 工具类, 包含若干 Kafka 操作方法
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class KafkaUtil {
    /**
     * 创建主题
     *
     * @param topic 主题名称
     * @return 主题创建结果对象
     */
    public static void createTopic(String topic) {
        createTopic(topic, 3);
    }

    /**
     * 创建主题
     *
     * @param topic         主题名称
     * @param numPartitions 主题分区数
     * @return 主题创建结果对象
     */
    public static void createTopic(String topic, int numPartitions) {
        createTopic(topic, numPartitions, (short) 2);
    }

    /**
     * 创建主题
     *
     * @param topic             主题名称
     * @param numPartitions     主题分区数
     * @param replicationFactor 主题分区副本数
     * @return 主题创建结果对象
     */
    public static CreateTopicsResult createTopic(String topic, int numPartitions, short replicationFactor) {
        try (var ac = KafkaClientBuilder.createAdminClient()) {
            return ac.createTopics(
                List.of(new NewTopic(topic, numPartitions, replicationFactor)),
                new CreateTopicsOptions().timeoutMs(5000).validateOnly(false));
        }
    }

    /**
     * 从消费者对象中读取指定数量的最新消息
     *
     * @param <K>      消息 Key 类型
     * @param <V>      消息 Value 类型
     * @param consumer {@link Consumer} 类型消费者对象
     * @param n        要获取消息的数量
     * @return {@link ConsumerRecord} 类型的对象集合, 表示获取的消息
     */
    public static <K, V> ConsumerRecords<K, V> pollLastNRecords(Consumer<K, V> consumer, int n) {
        return pollLastNRecords(consumer, n, true);
    }

    /**
     * 从消费者对象中读取指定数量的最新消息
     *
     * @param <K>      消息 Key 类型
     * @param <V>      消息 Value 类型
     * @param consumer {@link Consumer} 类型消费者对象
     * @param n        要获取消息的数量
     * @param commit   是否提交消息偏移量
     * @return {@link ConsumerRecords} 对象, 表示获取的消息
     */
    @SneakyThrows
    public static <K, V> ConsumerRecords<K, V> pollLastNRecords(Consumer<K, V> consumer, int n, boolean commit) {
        // 保存结果的集合对象
        var results = new ArrayDeque<ConsumerRecord<K, V>>();

        var receivedCount = 0;

        // 循环, 不断从 Broker 获取消息
        while (true) {
            // 获取一批消息对象
            var records = consumer.poll(Duration.ofSeconds(1));

            // 如果本次未获取到消息, 且上次循环已获取到消息, 表示所有的消息已被获取完毕, 退出循环
            if (records.isEmpty() && !results.isEmpty()) {
                break;
            }

            // 遍历获取的一批消息
            for (var r : records) {
                // 保证结果集合中元素的数量
                if (results.size() == n) {
                    results.removeFirst();
                }
                // 将消息保存到结果集合中
                results.addLast(r);

                // 每收到 10 条消息, 进行一次异步提交
                if (commit && receivedCount++ >= 10) {
                    receivedCount = 0;
                    consumer.commitAsync();
                }
            }
        }

        // 如果需要提交偏移量, 则对本次消费的偏移量进行提交
        if (commit) {
            // 进行一次同步提交, 保证正确提交偏移量
            consumer.commitSync(Duration.ofSeconds(1));
        }

        // 返回结果
        return new ConsumerRecords<>(results.stream().collect(
            Collectors.groupingBy(r -> new TopicPartition(r.topic(), r.partition()))));
    }

    /**
     * 通过消费者获取所有分区
     *
     * @param consumer 消费者对象
     * @return 以分区 ID 为 Key, {@link TopicPartition} 为 Value 的 {@link Map} 对象, 表示各个分区
     */
    public static Map<Integer, TopicPartition> partitionFromConsumer(Consumer<?, ?> consumer) {
        return consumer.assignment().stream().collect(Collectors.toMap(tp -> tp.partition(), tp -> tp));
    }

    /**
     * 通过消费者对象获取各个分区的偏移量
     *
     * @param consumer 消费者对象
     * @return 以 {@link TopicPartition} 为 Key, {@link OffsetAndMetadata} 为 Value 的 {@link Map} 对象,
     *         表示各个分区的偏移量
     */
    public static Map<TopicPartition, OffsetAndMetadata> partitionOffsetFromConsumer(Consumer<?, ?> consumer) {
        return consumer.assignment().stream().collect(
            Collectors.toMap(
                tp -> tp,
                tp -> new OffsetAndMetadata(consumer.position(tp))));
    }

    /**
     * 获取 {@link ConsumerRecords} 结果中的第一个元素
     *
     * @param <K>     消息 Key 类型
     * @param <V>     消息 Value 类型
     * @param records 消费者获取消息结果集合
     * @return 消息结果集合中的第一个元素
     */
    public static <K, V> Optional<ConsumerRecord<K, V>> firstResult(ConsumerRecords<K, V> records) {
        return StreamSupport.stream(records.spliterator(), false).findFirst();
    }

    /**
     * 获取本地文件中的 Avro Schema 对象
     *
     * @param schemaFile 保存 Avro Schema 的文件路径
     * @return {@link Schema} 对象, 表示 Apache Avro Schema 对象
     */
    @SneakyThrows
    public static Schema parseSchema(String schemaFile) {
        var parser = new Schema.Parser();
        return parser.parse(new File(schemaFile));
    }
}
