package alvin.docker.core.context;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@EntityScan(basePackages = { "alvin.docker.infra.model" })
@Configuration("conf/jpa")
@EnableJpaAuditing
@EnableTransactionManagement
public class JpaConfig {
}
