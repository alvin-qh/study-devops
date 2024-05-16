package alvin.study.maven.test.conf;

import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;

import alvin.study.maven.Application;

public class TestContextInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
        var properties = Application.getDefaultProperties();
        TestPropertyValues.of(properties).applyTo(applicationContext);
    }
}
