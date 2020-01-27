package alvin.docker.app.web.controller;

import alvin.docker.testing.WebTestSupport;
import org.junit.jupiter.api.Test;

class HomeControllerTest extends WebTestSupport {

    @Test
    void test_index() {
        get("/").exchange()
                .expectStatus().is2xxSuccessful();
    }
}
