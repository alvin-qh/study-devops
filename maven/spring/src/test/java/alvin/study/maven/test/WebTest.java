package alvin.study.maven.test;

import java.time.Duration;

import javax.servlet.ServletContext;

import com.google.common.base.Charsets;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.test.web.reactive.server.WebTestClient.RequestBodySpec;
import org.springframework.test.web.reactive.server.WebTestClient.RequestHeadersSpec;
import org.springframework.test.web.reactive.server.WebTestClient.RequestHeadersUriSpec;

@SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
@AutoConfigureWebTestClient
public abstract class WebTest extends IntegrationTest {
    @Autowired
    private WebTestClient webClient;

    @Autowired
    private ServletContext context;

    protected WebTestClient webClient() {
        return webClient.mutate().responseTimeout(Duration.ofMinutes(1)).build();
    }

    @SafeVarargs
    @SuppressWarnings("unchecked")
    private <T extends RequestHeadersSpec<?>, R extends RequestHeadersUriSpec<?>> T setup(R client, String url,
            Object... args) {
        return (T) client.uri(context.getContextPath() + url, args)
                .header(HttpHeaders.CONTENT_ENCODING, Charsets.UTF_8.name())
                .acceptCharset(Charsets.UTF_8);
    }

    protected RequestBodySpec post(String url, Object... args) {
        return ((RequestBodySpec) setup(webClient().post(), url, args))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .accept(MediaType.TEXT_HTML);
    }

    protected RequestBodySpec postJson(String url, Object... args) {
        return ((RequestBodySpec) setup(webClient().post(), url, args))
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON);
    }

    protected RequestHeadersSpec<?> get(String url, Object... args) {
        return setup(webClient().get(), url, args)
                .accept(MediaType.TEXT_HTML);
    }

    protected WebTestClient.RequestHeadersSpec<?> getJson(String url, Object... args) {
        return setup(webClient().get(), url, args)
                .accept(MediaType.APPLICATION_JSON);
    }

    protected RequestBodySpec put(String url, Object... args) {
        return ((RequestBodySpec) setup(webClient().put(), url, args))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .accept(MediaType.TEXT_HTML);
    }

    protected RequestBodySpec putJson(String url, Object... args) {
        return ((RequestBodySpec) setup(webClient().put(), url, args))
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON);
    }

    protected WebTestClient.RequestHeadersSpec<?> delete(String url, Object... args) {
        return setup(webClient().delete(), url, args)
                .accept(MediaType.TEXT_HTML);
    }

    protected WebTestClient.RequestHeadersSpec<?> deleteJson(String url, Object... args) {
        return setup(webClient().delete(), url, args)
                .accept(MediaType.APPLICATION_JSON);
    }
}
