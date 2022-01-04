package alvin.docker.infra.repo;

import alvin.docker.infra.model.FeedbackBuilder;
import alvin.docker.testing.IntegrationTestSupport;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;


class FeedbackRepositoryTest extends IntegrationTestSupport {

    @Inject
    private FeedbackRepository repository;

    @Test
    void test_create() {
        var entity = newBuilder(FeedbackBuilder.class).build();

        try (var ignore = beginTx()) {
            repository.save(entity);
        }
    }

    @Test
    void test_list() {
        try (var ignore = beginTx()) {
            newBuilder(FeedbackBuilder.class).create();
        }

        var all = repository.findAll();
        assertThat(all.size(), is(1));
    }
}
