package alvin.study.kafka;

import java.util.Map;
import java.util.Properties;

import com.google.common.io.Resources;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;

/**
 * 处理 Kafka 客户端配置项的工具类
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class KafkaClientConfig {
    /**
     * 从资源中读取配置项
     *
     * @param resourceName 资源名称
     * @return 资源中配置项组成的 {@link Properties} 对象
     */
    @SneakyThrows
    private static Properties loadConfig(String resourceName) {
        var props = new Properties();

        var resUrl = Resources.getResource(resourceName);
        try (var is = Resources.asByteSource(resUrl).openStream()) {
            props.load(is);
        }
        return props;
    }

    /**
     * 将配置项 {@link Properties} 对象和另一个 {@link Map} 对象合并
     *
     * @param props   保存配置项的 {@link Properties} 对象
     * @param extConf 保存被合并配置项的 {@link Map} 对象
     * @return 合并了 {@code extConf} 参数的 {@code props} 参数引用
     */
    private static Properties mergeConfig(Properties props, Map<?, ?> extConf) {
        if (extConf != null) {
            extConf.forEach((k, v) -> {
                if (k instanceof String && v instanceof String) {
                    props.setProperty((String) k, (String) v);
                }
            });
        }
        return props;
    }

    /**
     * 读取公共配置信息
     *
     * @param extConf 附加配置信息
     * @return 保存公共配置信息的 {@link Properties} 对象
     */
    public static Properties loadCommonConfig(Map<String, String> extConf) {
        return mergeConfig(loadConfig("conf/common.properties"), extConf);
    }

    /**
     * 读取生产者配置信息
     *
     * @param extConf 附加配置信息
     * @return 保存生产者配置信息的 {@link Properties} 对象
     */
    public static Properties loadProducerConfig(Map<String, String> extConf) {
        var props = mergeConfig(loadCommonConfig(Map.of()), loadConfig("conf/producer.properties"));
        return mergeConfig(props, extConf);
    }

    /**
     * 读取消费者配置信息
     *
     * @param extConf 附加配置信息
     * @return 保存消费者配置信息的 {@link Properties} 对象
     */
    public static Properties loadConsumerConfig(Map<String, String> extConf) {
        var props = mergeConfig(loadCommonConfig(Map.of()), loadConfig("conf/consumer.properties"));
        return mergeConfig(props, extConf);
    }
}
