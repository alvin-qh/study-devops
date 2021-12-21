package alvin.docker.testing;

import com.google.common.base.Charsets;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

import javax.inject.Inject;
import javax.servlet.ServletContext;
import java.time.Duration;

@AutoConfigureWebTestClient
public class WebTestSupport extends IntegrationTestSupport {

    @Inject
    private WebTestClient webClient;

    @Inject
    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    private ServletContext context;

    protected WebTestClient webClient() {
        return webClient.mutate().responseTimeout(Duration.ofMinutes(1)).build();
    }

    protected String url(String url) {
        return context.getContextPath() + url;
    }

    @SuppressWarnings("unchecked")
    private <T extends WebTestClient.RequestHeadersSpec<?>> T setupClient(
            WebTestClient.RequestHeadersUriSpec<?> client, String url, Object... args) {
        return (T) client.uri(url(url), args)
                .acceptCharset(Charsets.UTF_8);
    }

    protected WebTestClient.RequestBodySpec post(String url, Object... args) {
        return ((WebTestClient.RequestBodySpec) setupClient(webClient().post(), url, args))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .accept(MediaType.TEXT_HTML);
    }

    protected WebTestClient.RequestBodySpec postJson(String url, Object... args) {
        return ((WebTestClient.RequestBodySpec) setupClient(webClient().post(), url, args))
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON);
    }

    protected WebTestClient.RequestHeadersSpec<?> get(String url, Object... args) {
        return ((WebTestClient.RequestBodySpec) setupClient(webClient().get(), url, args))
                .accept(MediaType.TEXT_HTML);
    }

    protected WebTestClient.RequestHeadersSpec<?> getJson(String url, Object... args) {
        return ((WebTestClient.RequestBodySpec) setupClient(webClient().get(), url, args))
                .accept(MediaType.APPLICATION_JSON);
    }

    protected WebTestClient.RequestBodySpec put(String url, Object... args) {
        return ((WebTestClient.RequestBodySpec) setupClient(webClient().put(), url, args))
                .accept(MediaType.TEXT_HTML)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED);
    }

    protected WebTestClient.RequestBodySpec putJson(String url, Object... args) {
        return ((WebTestClient.RequestBodySpec) setupClient(webClient().put(), url, args))
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON);
    }

    protected WebTestClient.RequestHeadersSpec<?> delete(String url, Object... args) {
        return ((WebTestClient.RequestBodySpec) setupClient(webClient().delete(), url, args))
                .accept(MediaType.TEXT_HTML);
    }

    protected WebTestClient.RequestHeadersSpec<?> deleteJson(String url, Object... args) {
        return ((WebTestClient.RequestBodySpec) setupClient(webClient().delete(), url, args))
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON);
    }
}
