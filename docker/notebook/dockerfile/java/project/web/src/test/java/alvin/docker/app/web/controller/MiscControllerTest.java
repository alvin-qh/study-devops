package alvin.docker.app.web.controller;

import alvin.docker.testing.WebTestSupport;
import org.junit.jupiter.api.Test;

class MiscControllerTest extends WebTestSupport {

    @Test
    void test_version() {
        getJson("/d/version").exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.version").isEqualTo("@version@")
                .jsonPath("$.zone").isEqualTo("Asia/Shanghai");
    }
}
