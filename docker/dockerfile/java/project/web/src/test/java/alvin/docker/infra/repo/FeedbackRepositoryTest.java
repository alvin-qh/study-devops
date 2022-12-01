package alvin.docker.infra.repo;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import javax.inject.Inject;

import org.junit.jupiter.api.Test;

import alvin.docker.infra.model.FeedbackBuilder;
import alvin.docker.infra.repository.FeedbackRepository;
import alvin.docker.testing.IntegrationTestSupport;


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
