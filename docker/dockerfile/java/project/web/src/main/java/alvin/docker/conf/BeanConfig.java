package alvin.docker.conf;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import alvin.docker.core.http.model.ApplicationInfo;

@Configuration("conf/bean")
public class BeanConfig {

    @Bean
    @ConfigurationProperties(prefix = "application")
    ApplicationInfo applicationInfo() {
        return new ApplicationInfo();
    }
}
