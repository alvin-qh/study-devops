package alvin.study.kafka;

import java.time.Duration;
import java.util.ArrayDeque;
import java.util.List;

import org.apache.kafka.clients.admin.CreateTopicsOptions;
import org.apache.kafka.clients.admin.CreateTopicsResult;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;

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
    public static <K, V> List<ConsumerRecord<K, V>> pollLastNRecords(Consumer<K, V> consumer, int n) {
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
     * @return {@link ConsumerRecord} 类型的对象集合, 表示获取的消息
     */
    @SneakyThrows
    public static <K, V> List<ConsumerRecord<K, V>> pollLastNRecords(Consumer<K, V> consumer, int n, boolean commit) {
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
        return List.copyOf(results);
    }
}
