package alvin.study.maven.api.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;
import org.springframework.core.ParameterizedTypeReference;

import alvin.study.maven.api.model.HelloDto;
import alvin.study.maven.common.model.Response;
import alvin.study.maven.test.WebTest;

class HelloControllerTest extends WebTest {
    @Test
    void shouldHelloReturnName() {
        var resp = getJson("/hello").exchange()
                .expectStatus().is2xxSuccessful()
                .expectBody(new ParameterizedTypeReference<Response<HelloDto>>() {
                }).returnResult()
                .getResponseBody();

        assertNull(resp.getError());
        assertEquals("Alvin", resp.getPayload().getName());
        assertTrue(resp.getPayload().getTimestamp().compareTo(LocalDateTime.now(ZoneOffset.UTC)) <= 0);
    }
}
