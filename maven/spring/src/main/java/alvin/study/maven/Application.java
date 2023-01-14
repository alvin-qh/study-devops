package alvin.study.maven;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.tuple.Pair;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.PropertySource;
import org.springframework.scheduling.annotation.EnableAsync;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;

@SpringBootApplication
@Configuration
@EnableAutoConfiguration
@EnableAsync
@PropertySource("classpath:application.yml")
public class Application {
    public static void main(String[] args) {
        // 创建 SpringBoot Application 对象
        var app = new SpringApplication(Application.class);
        // 设置默认应用属性
        app.setDefaultProperties(Map.copyOf(getDefaultProperties()));

        // 启动 Application
        app.run(args);
    }

    /**
     * 自定义 Jackson mapper 规则
     */
    @Bean
    @Primary
    public Jackson2ObjectMapperBuilderCustomizer addJacksonMapperCustomizer() {
        return builder -> builder.featuresToDisable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .serializationInclusion(JsonInclude.Include.NON_NULL);
    }

    /**
     * 获取默认应用属性
     *
     * @param additionalProperties 扩展熟悉列表
     * @return 包含默认属性的 Map 对象
     */
    @SafeVarargs
    public static Map<String, String> getDefaultProperties(Pair<String, String>... additionalProperties) {
        var properties = new HashMap<String, String>();

        // SpringBoot 默认配置
        properties.put("spring.main.banner-mode", "off"); // 不显示欢迎横幅
        properties.put("spring.main.allow-bean-definition-overriding", "true"); // 允许覆盖 bean 定义

        // 追加额外配置
        for (var property : additionalProperties) {
            properties.put(property.getKey(), property.getValue());
        }

        return properties;
    }
}
