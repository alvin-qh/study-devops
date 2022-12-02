package alvin.docker.app.endpoint;

import java.util.List;
import java.util.stream.Collectors;

import javax.validation.Valid;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import alvin.docker.app.domain.service.FeedbackService;
import alvin.docker.app.endpoint.mapper.FeedbackMapper;
import alvin.docker.app.endpoint.model.FeedbackDto;
import alvin.docker.app.endpoint.model.FeedbackForm;
import lombok.RequiredArgsConstructor;

/**
 * Feedback Controller 类型
 */
@Validated
@RestController
@RequestMapping("/api/feedback")
@RequiredArgsConstructor
public class FeedbackController {
    // 注入服务对象
    private final FeedbackService feedbackService;

    // 注入对象转换器
    private final FeedbackMapper feedbackMapper;

    /**
     * 获取 {@link alvin.docker.infra.model.Feedback Feedback} 查询结果列表
     *
     * @return {@link FeedbackDto} 集合
     */
    @GetMapping
    @ResponseBody
    List<FeedbackDto> index() {
        return feedbackService.list().stream()
                .map(feedbackMapper::toDto)
                .collect(Collectors.toList());
    }

    /**
     * 创建一个 {@link alvin.docker.infra.model.Feedback Feedback} 实体对象
     */
    @PostMapping
    @ResponseBody
    void create(@RequestBody @Valid FeedbackForm form) {
        var feedback = feedbackMapper.toEntity(form);
        feedbackService.create(feedback);
    }

    /**
     * 删除一个 {@link alvin.docker.infra.model.Feedback Feedback} 实体对象
     *
     * @param id 实体对象 {@code id}
     */
    @ResponseBody
    @DeleteMapping("/{id}")
    void delete(@PathVariable("id") Long id) {
        feedbackService.delete(id);
    }
}
