package alvin.study.kafka;

import static org.assertj.core.api.BDDAssertions.then;

import java.util.Map;

import org.junit.jupiter.api.Test;

class KafkaClientConfigTest {
    @Test
    void loadCommonConfig_shouldLoadCommonConfig() throws Exception {
        var props = KafkaClientConfig.loadCommonConfig(Map.of("a", "b"));

        then(props.getProperty("bootstrap.servers")).isEqualTo("localhost:19092,localhost:19093,localhost:19094");
        then(props.getProperty("a")).isEqualTo("b");
    }
}
