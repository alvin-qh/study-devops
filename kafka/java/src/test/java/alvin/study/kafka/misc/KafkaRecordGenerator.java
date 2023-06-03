package alvin.study.kafka.misc;

import java.util.UUID;

import org.apache.kafka.clients.producer.ProducerRecord;

/**
 * 用于生成测试消息的工具类
 */
public class KafkaRecordGenerator {
    // 消息 Key 的前缀
    private final String keyPrefix;
    // 消息 Value 的前缀
    private final String valuePrefix;

    /**
     * 构造器, 设置消息默认的 Key 和 Value 前缀
     */
    public KafkaRecordGenerator() {
        this("key-", "value-");
    }

    /**
     * 构造器, 设置消息 Key 和 Value 的前缀
     *
     * @param keyPrefix   Key 前缀
     * @param valuePrefix Value 前缀
     */
    public KafkaRecordGenerator(String keyPrefix, String valuePrefix) {
        this.keyPrefix = keyPrefix;
        this.valuePrefix = valuePrefix;
    }

    /**
     * 生成一个 Key 和 Value 为随机值的消息对象
     *
     * @param topic 消息的主题
     * @return {@link ProducerRecord} 对象, 表示一个 Kafka 消息
     */
    public ProducerRecord<String, String> generate(String topic) {
        var key = keyPrefix + UUID.randomUUID().toString();
        var value = valuePrefix + UUID.randomUUID().toString();

        return new ProducerRecord<>(topic, key, value);
    }
}
