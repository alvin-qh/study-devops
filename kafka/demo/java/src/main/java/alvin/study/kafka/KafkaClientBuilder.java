package alvin.study.kafka;

import java.net.InetAddress;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRebalanceListener;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;

/**
 * Kafka 客户端工厂类
 *
 * <p>
 * 用于创建 Kafka 客户端, 包括: 管理客户端, 生产者客户端和消费者客户端
 * </p>
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class KafkaClientBuilder {
    /**
     * 创建管理客户端
     *
     * @return {@link AdminClient} 类型对象, 表示 Kafka 管理客户端
     */
    public static AdminClient createAdminClient() {
        var conf = KafkaClientConfig.loadCommonConfig(Map.of());
        return AdminClient.create(conf);
    }

    /**
     * 创建生产者客户端
     *
     * @param <K> Key 的类型要和生产者配置中的 {@code key.serializer} 配置项对应
     * @param <V> Value 的类型要和生产者配置中的 {@code value.serializer} 配置项对应
     * @return {@link KafkaProducer} 类型对象, 表示生产者对象
     */
    public static <K, V> Producer<K, V> createProducer() {
        return createProducer(Map.of());
    }

    /**
     * 创建生产者客户端
     *
     * @param <K>      Key 的类型要和生产者配置中的 {@code key.serializer} 配置项对应
     * @param <V>      Value 的类型要和生产者配置中的 {@code value.serializer} 配置项对应
     * @param extProps 扩展配置项字典
     * @return {@link KafkaProducer} 类型对象, 表示生产者对象
     */
    @SneakyThrows
    public static <K, V> Producer<K, V> createProducer(Map<String, String> extProps) {
        if (extProps.containsKey(ProducerConfig.CLIENT_ID_CONFIG)) {
            extProps = new HashMap<>(extProps);
            extProps.put(ProducerConfig.CLIENT_ID_CONFIG, InetAddress.getLocalHost().getHostName());
        }

        return new KafkaProducer<>(KafkaClientConfig.loadProducerConfig(extProps));
    }

    /**
     * 创建消费者客户端, 监听指定主题
     *
     * @param <K>     Key 的类型要和消费者配置中的 {@code key.deserializer} 配置项对应
     * @param <V>     Value 的类型要和消费者配置中的 {@code value.deserializer} 配置项对应
     * @param topic   要订阅的主题名称
     * @param groupId 要加入的消费组
     * @return {@link KafkaProducer} 类型对象, 表示消费者对象
     */
    public static <K, V> Consumer<K, V> createConsumer(String topic, String groupId) {
        return createConsumer(topic, groupId, Map.of());
    }

    /**
     * 创建消费者客户端, 监听指定主题
     *
     * @param <K>      Key 的类型要和消费者配置中的 {@code key.deserializer} 配置项对应
     * @param <V>      Value 的类型要和消费者配置中的 {@code value.deserializer} 配置项对应
     * @param topic    要订阅的主题名称
     * @param groupId  要加入的消费组
     * @param extProps 扩展配置项字典
     * @return {@link KafkaProducer} 类型对象, 表示消费者对象
     */
    public static <K, V> Consumer<K, V> createConsumer(String topic, String groupId, Map<String, String> extProps) {
        return createConsumer(topic, groupId, null, extProps);
    }

    /**
     * 创建消费者客户端, 监听指定主题
     *
     * @param <K>               Key 的类型要和消费者配置中的 {@code key.deserializer} 配置项对应
     * @param <V>               Value 的类型要和消费者配置中的 {@code value.deserializer} 配置项对应
     * @param topic             要订阅的主题名称
     * @param groupId           要加入的消费组
     * @param rebalanceListener 消费者再平衡监听器对象, {@code null} 表示无需监听
     * @return {@link KafkaProducer} 类型对象, 表示消费者对象
     */
    public static <K, V> Consumer<K, V> createConsumer(
            String topic,
            String groupId,
            ConsumerRebalanceListener rebalanceListener) {
        return createConsumer(topic, groupId, rebalanceListener, Map.of());
    }

    /**
     * 创建消费者客户端, 监听指定主题
     *
     * @param <K>               Key 的类型要和消费者配置中的 {@code key.deserializer} 配置项对应
     * @param <V>               Value 的类型要和消费者配置中的 {@code value.deserializer} 配置项对应
     * @param topic             要订阅的主题名称
     * @param groupId           要加入的消费组
     * @param rebalanceListener 消费者再平衡监听器对象, {@code null} 表示无需监听
     * @param extProps          扩展配置项字典
     * @return {@link KafkaProducer} 类型对象, 表示消费者对象
     */
    @SneakyThrows
    public static <K, V> Consumer<K, V> createConsumer(
            String topic,
            String groupId,
            ConsumerRebalanceListener rebalanceListener,
            Map<String, String> extProps) {
        extProps = new HashMap<>(extProps);
        extProps.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);

        var consumer = new KafkaConsumer<K, V>(KafkaClientConfig.loadConsumerConfig(extProps));
        if (rebalanceListener != null) {
            consumer.subscribe(List.of(topic), rebalanceListener);
        } else {
            consumer.subscribe(List.of(topic));
        }
        return consumer;
    }
}
