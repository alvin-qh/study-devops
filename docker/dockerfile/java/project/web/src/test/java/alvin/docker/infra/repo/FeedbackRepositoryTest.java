package alvin.docker.infra.repo;

import alvin.docker.infra.model.FeedbackBuilder;
import alvin.docker.testing.IntegrationTestSupport;
import lombok.val;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;


class FeedbackRepositoryTest extends IntegrationTestSupport {

    @Inject
    private FeedbackRepository repository;

    @Test
    void test_create() {
        final val entity = newBuilder(FeedbackBuilder.class).build();

        try (val ignore = beginTx()){
            repository.save(entity);
        }
    }

    @Test
    void test_list() {
        try (val ignore = beginTx()) {
            newBuilder(FeedbackBuilder.class).create();
        }

        final val all = repository.findAll();
        assertThat(all.size(), is(1));
    }
}
