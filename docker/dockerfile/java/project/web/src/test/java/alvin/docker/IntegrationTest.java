package alvin.docker;

import java.time.Duration;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.servlet.ServletContext;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.test.web.reactive.server.WebTestClient.RequestBodySpec;
import org.springframework.test.web.reactive.server.WebTestClient.RequestHeadersSpec;
import org.springframework.test.web.reactive.server.WebTestClient.RequestHeadersUriSpec;

import com.google.common.base.Charsets;

import alvin.docker.builder.Builder;
import alvin.docker.common.TableCleaner;
import alvin.docker.common.TestingTransaction;
import alvin.docker.common.TestingTransactionManager;
import alvin.docker.conf.TestConfig;
import alvin.docker.core.context.Context;
import alvin.docker.core.context.CustomRequestAttributes;
import alvin.docker.core.context.WebContext;
import alvin.docker.infra.model.BaseEntity;
import lombok.SneakyThrows;

@ActiveProfiles("test")
@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = TestConfig.class, webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@AutoConfigureWebTestClient
public abstract class IntegrationTest extends UnitTest {
    @Autowired
    private WebTestClient client;

    /**
     * 注入 JPA 实体管理器对象
     *
     * <p>
     * {@link EntityManager} 对所有 JPA 的实体进行管理, 对其进行状态和增删改查操作的管理.
     * {@link PersistenceContext @PersistenceContext} 注解用于注入 {@link EntityManager}
     * 对象
     * </p>
     */
    @PersistenceContext
    private EntityManager em;

    /**
     * 注入测试用事务管理器对象
     *
     * <p>
     * 在测试时, 有时候不方便应用
     * {@link org.springframework.transaction.annotation.Transactional @Transactional}
     * 注解, 此时可以使用该事务管理器手动启动和结束事务
     * </p>
     *
     * <p>
     * 该对象由
     * {@link TestingConfig#testingTransactionManager(org.springframework.transaction.PlatformTransactionManager)
     * TestingConfig.testingTransactionManager(PlatformTransactionManager)} 方法产生
     * </p>
     */
    @Autowired
    private TestingTransactionManager txManager;

    /**
     * 用于在每次测试开始前, 将测试数据表全部清空
     *
     * @see TableCleaner#clearAllTables(String...)
     */
    @Autowired
    private TableCleaner tableCleaner;

    /**
     * 请求上下文对象
     *
     * @see alvin.study.conf.ContextConfig#context()
     */
    @Autowired
    private Context context;

    /**
     * Servlet 上下文对象
     */
    @Autowired
    private ServletContext servletContext;

    /**
     * Bean 工厂类
     *
     * <p>
     * {@link AutowireCapableBeanFactory} 用于从 Bean 容器中创建一个 Bean 对象或者为一个已有的 Bean
     * 对象注入所需的参数
     * </p>
     */
    @Autowired
    private AutowireCapableBeanFactory beanFactory;

    /**
     * 开启事务
     *
     * @param readOnly 事务的只读性
     * @return 用于测试的事务管理器对象
     */
    protected TestingTransaction beginTx(boolean readOnly) {
        return txManager.begin(readOnly);
    }

    /**
     * 清理 {@link EntityManager} 缓存
     *
     * <p>
     * 清理 {@link EntityManager} 中的一级缓存, 之后所有的实体对象会重新从数据库而非缓存中获取, 这样可以真实的反应数据库的变化
     * </p>
     */
    protected void clearEntityManager() {
        em.clear();
    }

    /**
     * 创建实体类型的构建器对象
     *
     * @param <T>         构建器类型, 即 {@link Builder Builder} 类的子类型
     * @param builderType 构建器类型的 {@link Class} 对象
     * @return 构建器实例
     */
    @SneakyThrows
    protected <T extends Builder<?>> T newBuilder(Class<T> builderType) {
        // 创建新的 Builder 对象
        var builder = builderType.getConstructor().newInstance();

        // 对已有对象进行注入操作
        beanFactory.autowireBean(builder);
        return builder;
    }

    /**
     * 刷新一个非受控的实体对象变为受控对象
     *
     * @param entity 非受控对象
     */
    @SuppressWarnings("unchecked")
    protected <T extends BaseEntity> T refreshEntity(T entity) {
        return (T) em.find(entity.getClass(), entity.getId());
    }

    /**
     * <p>
     * 该方法中为当前请求上下文注册了 {@link Context} 对象
     * </p>
     *
     * @see IntegrationTest#newBuilder(Class)
     *
     * @see alvin.study.infra.entity.common.TenantedEntity#orgId
     * @see alvin.study.infra.entity.common.AuditedEntity#createdBy
     * @see alvin.study.infra.entity.common.AuditedEntity#updatedBy
     */
    @BeforeEach
    protected void beforeEach() {
        // 将除了 schema_version 以外的表内容清空
        tableCleaner.clearAllTables("schema_version");

        // 构建请求上下文, 之后 context 字段方能生效
        CustomRequestAttributes.register(new WebContext());
    }

    /**
     * 每次测试结束, 进行清理工作
     */
    @AfterEach
    protected void afterEach() {
        CustomRequestAttributes.unregister();
    }

    /**
     * 获取请求上下文对象
     *
     * @return {@link Context} 对象
     */
    public Context getContext() {
        return context;
    }

    /**
     * 实例化一个测试客户端
     *
     * @return {@link WebTestClient} 类型对象
     */
    protected WebTestClient webClient() {
        return client
                // 对 client 字段进行更新操作, 返回
                // org.springframework.test.web.reactive.server.WebTestClient.Builder 对象
                .mutate()
                // 设置请求超时
                .responseTimeout(Duration.ofMinutes(1))
                // 创建新的 WebTestClient 对象
                .build();
    }

    /**
     * 设置测试客户端
     *
     * @param <T>          Response 类型
     * @param <R>          Request 类型
     *
     * @param spec         请求对象
     * @param url          请求地址
     * @param uriVariables 在 URL 中包含的请求参数值
     *
     * @return {@link RequestHeadersSpec} 对象, 用于发送测试请求
     */
    @SuppressWarnings("unchecked")
    private <T extends RequestHeadersSpec<?>> T setupClient(
            RequestHeadersUriSpec<?> spec, String url, Object... uriVariables) {
        return (T) spec.uri(servletContext.getContextPath() + url, uriVariables)
                .acceptCharset(Charsets.UTF_8);
    }

    /**
     * 发送 json 类型的 {@code post} 请求
     *
     * @param url          请求地址
     * @param uriVariables 在 URL 中包含的请求参数值
     *
     * @return {@link RequestBodySpec} 请求类型
     */
    protected RequestBodySpec postJson(String url, Object... uriVariables) {
        return ((RequestBodySpec) setupClient(webClient().post(), url, uriVariables))
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON);
    }

    /**
     * 发送 json 类型的 {@code get} 请求
     *
     * @param url          请求地址
     * @param uriVariables 在 URL 中包含的请求参数值
     *
     * @return {@link RequestHeadersSpec} 对象, 用于发送测试请求
     */
    protected RequestHeadersSpec<?> getJson(String url, Object... uriVariables) {
        return ((RequestBodySpec) setupClient(webClient().get(), url, uriVariables))
                .accept(MediaType.APPLICATION_JSON);
    }

    /**
     * 发送 json 类型的 {@code put} 请求
     *
     * @param url          请求地址
     * @param uriVariables 在 URL 中包含的请求参数值
     *
     * @return {@link RequestBodySpec} 请求类型
     */
    protected RequestBodySpec putJson(String url, Object... uriVariables) {
        return ((RequestBodySpec) setupClient(webClient().put(), url, uriVariables))
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON);
    }

    /**
     * 发送 json 类型的 {@code delete} 请求
     *
     * @param url          请求地址
     * @param uriVariables 在 URL 中包含的请求参数值
     *
     * @return {@link RequestHeadersSpec} 请求类型
     */
    protected RequestHeadersSpec<?> deleteJson(String url, Object... uriVariables) {
        return ((RequestBodySpec) setupClient(webClient().delete(), url, uriVariables))
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON);
    }
}
