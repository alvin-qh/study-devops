package alvin.docker.conf;

import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * 初始化测试上下文
 *
 * <p>
 * 该类型用于在测试执行前, 进行额外的配置工作
 * </p>
 *
 * <p>
 * 在 {@link alvin.study.IntegrationTest} 类型上通过
 * {@link org.springframework.test.context.ContextConfiguration @ContextConfiguration}
 * 注解的 {@code initializers} 参数, 指定在测试前执行此类的
 * {@link TestingContextInitializer#initialize(ConfigurableApplicationContext)}
 * 方法
 * </p>
 */
public class TestingContextInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
    /**
     * 对所给的 {@link ConfigurableApplicationContext} 对象进行初始化操作
     */
    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
    }
}
