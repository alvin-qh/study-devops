package alvin.docker.infra.repository;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import alvin.docker.IntegrationTest;
import alvin.docker.builder.FeedbackBuilder;

class FeedbackRepositoryTest extends IntegrationTest {
    @Autowired
    private FeedbackRepository repository;

    @Test
    void list_shouldListEntities() {
        try (var ignore = beginTx(false)) {
            newBuilder(FeedbackBuilder.class).create();
        }

        var entities = repository.findAll();
        assertThat(entities.size(), is(1));
    }
}
