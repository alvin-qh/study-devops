package alvin.study.kafka;

import java.io.IOException;
import java.util.Map;

import org.apache.kafka.clients.admin.AdminClient;

public class KafkaAdminClient {
    private AdminClient aClient;

    public KafkaAdminClient() throws IOException {
        var conf = KafkaClientConfig.loadCommonConfig(Map.of());
        this.aClient = AdminClient.create(conf);
    }
}
