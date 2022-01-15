package alvin.study.maven.api.controller;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import alvin.study.maven.api.model.HelloDto;
import alvin.study.maven.common.model.Response;

/**
 * 测试控制器，输出一个简单 json
 */
@RestController
@RequestMapping("/hello")
public class HelloController {
    @GetMapping
    Response<HelloDto> hello() {
        // 组装 response 对象
        return Response.ok(new HelloDto("Alvin", LocalDateTime.now(ZoneOffset.UTC)));
    }
}
