package alvin.study.kafka;

import java.io.IOException;
import java.util.Map;
import java.util.Properties;

import com.google.common.io.Resources;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class KafkaClientConfig {
    private static Properties loadConfig(String resourceName) throws IOException {
        var props = new Properties();

        var resUrl = Resources.getResource(resourceName);
        try (var is = Resources.asByteSource(resUrl).openStream()) {
            props.load(is);
        }
        return props;
    }

    private static Properties mergeConfig(Properties props, Map<String, String> extConf) {
        if (extConf != null) {
            extConf.forEach(props::setProperty);
        }
        return props;
    }

    public static Properties loadCommonConfig(Map<String, String> extConf) throws IOException {
        return mergeConfig(loadConfig("conf/common.properties"), extConf);
    }

    public static Properties loadProducerConfig(Map<String, String> extConf) throws IOException {
        return mergeConfig(loadConfig("conf/producer.properties"), extConf);
    }

    public static Properties loadConsumerConfig(Map<String, String> extConf) throws IOException {
        return mergeConfig(loadConfig("conf/consumer.properties"), extConf);
    }
}
