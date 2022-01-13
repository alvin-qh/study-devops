package alvin.study.maven.api.controller;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import alvin.study.maven.api.model.HelloDto;
import alvin.study.maven.common.model.Response;

@RestController
@RequestMapping("/hello")
public class HelloController {
    @GetMapping
    Response<HelloDto> hello() {
        return new Response<>(null, new HelloDto("Alvin", LocalDateTime.now(ZoneOffset.UTC)));
    }
}
