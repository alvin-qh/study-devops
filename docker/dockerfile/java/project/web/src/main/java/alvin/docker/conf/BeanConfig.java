package alvin.docker.conf;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import alvin.docker.core.http.model.ApplicationInfo;

/**
 * Java Bean 配置类
 */
@Configuration("conf/bean")
public class BeanConfig {
    /**
     * 获取 {@link ApplicationInfo} 对象, 对应 {@code classpath:application.yml} 中的
     * {@code application} 配置项内容
     *
     * @return {@link ApplicationInfo} 对象
     */
    @Bean
    @ConfigurationProperties(prefix = "application")
    ApplicationInfo applicationInfo() {
        return new ApplicationInfo();
    }
}
