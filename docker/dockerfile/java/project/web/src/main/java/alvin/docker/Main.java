package alvin.docker;

import lombok.val;
import org.springframework.boot.Banner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.EncodedResourceResolver;
import org.springframework.web.servlet.resource.PathResourceResolver;

import java.util.Properties;

@Configuration
@PropertySource("classpath:application.yml")
@SpringBootApplication
@EnableTransactionManagement
@EntityScan(basePackages = {"alvin.docker.infra.model"})
@EnableJpaAuditing
@EnableAsync
public class Main implements WebMvcConfigurer {
    private static final String TABLE_SCHEMA_VERSION = "schema_version";
    private static final int STATIC_RESOURCE_CACHE_PERIOD = 3600;

    public static void main(String[] args) {
        val application = new SpringApplication(Main.class);

        application.setBannerMode(Banner.Mode.OFF);

        val props = new Properties();
        props.put("spring.datasource.hikari.pool-name", "demo.docker.alvin");
        props.put("spring.datasource.hikari.auto-commit", "true");

        props.put("spring.jpa.show-sql", "false");
        props.put("spring.jpa.open-in-view", "false");
        props.put("spring.jpa.hibernate.ddl-auto", "none");
        props.put("spring.jpa.properties.hibernate.enable_lazy_load_no_trans", "true");
        props.put("spring.jpa.properties.hibernate.dialect", "org.hibernate.dialect.H2Dialect");

        props.put("spring.flyway.locations", "classpath:migrations");
        props.put("spring.flyway.table", TABLE_SCHEMA_VERSION);

        application.setDefaultProperties(props);
        application.run(args);
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/static/**")
                .addResourceLocations("classpath:/static/")
                .setCachePeriod(STATIC_RESOURCE_CACHE_PERIOD)
                .resourceChain(true)
                .addResolver(new PathResourceResolver())
                .addResolver(new EncodedResourceResolver());
    }
}

