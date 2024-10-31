package alvin.docker.app.endpoint;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;

import alvin.docker.IntegrationTest;
import alvin.docker.app.endpoint.model.FeedbackDto;
import alvin.docker.app.endpoint.model.FeedbackForm;
import alvin.docker.builder.FeedbackBuilder;
import alvin.docker.core.http.model.ResponseWrapper;
import alvin.docker.infra.model.Feedback;
import alvin.docker.infra.repository.FeedbackRepository;

public class FeedbackControllerTest extends IntegrationTest {
    @Autowired
    private FeedbackRepository feedbackRepository;

    @Test
    void index_shouldReturnSuccess() {
        try (var tx = beginTx(false)) {
            for (var i = 0; i < 10; i++) {
                newBuilder(FeedbackBuilder.class).create();
            }
        }
        clearEntityManager();

        var resp = getJson("/api/feedback")
                .exchange()
                .expectStatus().isOk()
                .expectBody(new ParameterizedTypeReference<ResponseWrapper<List<FeedbackDto>>>() {
                })
                .returnResult()
                .getResponseBody();

        assertThat(resp.getCode(), is(equalTo(0)));
        assertThat(resp.getPath(), is(equalTo("/api/feedback")));
        assertThat(resp.getPayload(), is(hasSize(10)));
    }

    @Test
    void create_shouldReturnSuccess() {
        var form = new FeedbackForm("test-title", "test-content");

        var resp = postJson("/api/feedback")
                .bodyValue(form)
                .exchange()
                .expectStatus().isOk()
                .expectBody(new ParameterizedTypeReference<ResponseWrapper<Void>>() {
                })
                .returnResult()
                .getResponseBody();

        assertThat(resp.getCode(), is(equalTo(0)));
        assertThat(resp.getPath(), is(equalTo("/api/feedback")));

        var feedbacks = feedbackRepository.findAll();
        assertThat(feedbacks, is(hasSize(1)));
        assertThat(feedbacks.get(0).getTitle(), is(equalTo("test-title")));
        assertThat(feedbacks.get(0).getContent(), is(equalTo("test-content")));
    }

    @Test
    void delete_shouldReturnSuccess() {
        Feedback feedback;
        try (var tx = beginTx(false)) {
            feedback = newBuilder(FeedbackBuilder.class).create();
        }
        clearEntityManager();

        var resp = deleteJson("/api/feedback/{0}", feedback.getId())
                .exchange()
                .expectStatus().isOk()
                .expectBody(new ParameterizedTypeReference<ResponseWrapper<Void>>() {
                })
                .returnResult()
                .getResponseBody();

        assertThat(resp.getCode(), is(equalTo(0)));
        assertThat(resp.getPath(), is(equalTo("/api/feedback/1")));

        var feedbacks = feedbackRepository.findAll();
        assertThat(feedbacks, is(hasSize(0)));
    }
}
