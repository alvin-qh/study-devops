package alvin.docker.app.domain.service;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import alvin.docker.IntegrationTest;
import alvin.docker.builder.FeedbackBuilder;
import alvin.docker.infra.model.Feedback;

public class FeedbackServiceTest extends IntegrationTest {
    @Autowired
    private FeedbackService feedbackService;

    @Test
    void create_shouldInsertEntity() {
        var entity = new Feedback();
        entity.setTitle("test-title");
        entity.setContent("test-content");

        feedbackService.create(entity);

        var entities = feedbackService.list();
        assertThat(entities, is(hasSize(1)));
    }

    @Test
    void delete_shouldRemoveEntity() {
        Feedback feedback;
        try (var tx = beginTx(false)) {
            feedback = newBuilder(FeedbackBuilder.class).create();
        }
        clearEntityManager();

        feedbackService.delete(feedback.getId());

        var entities = feedbackService.list();
        assertThat(entities, is(empty()));
    }
}
