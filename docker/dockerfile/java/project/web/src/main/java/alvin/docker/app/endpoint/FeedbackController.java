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
import org.springframework.web.bind.annotation.RestController;

import alvin.docker.app.domain.service.FeedbackService;
import alvin.docker.app.endpoint.mapper.FeedbackMapper;
import alvin.docker.app.endpoint.model.FeedbackDto;
import alvin.docker.app.endpoint.model.FeedbackForm;
import lombok.RequiredArgsConstructor;

@Validated
@RestController
@RequestMapping("/api/feedback")
@RequiredArgsConstructor
public class FeedbackController {
    private final FeedbackService feedbackService;
    private final FeedbackMapper feedbackMapper;

    @GetMapping
    List<FeedbackDto> index() {
        return feedbackService.list().stream()
                .map(feedbackMapper::toDto)
                .collect(Collectors.toList());
    }

    @PostMapping
    void create(@RequestBody @Valid FeedbackForm form) {
        var feedback = feedbackMapper.toEntity(form);
        feedbackService.create(feedback);
    }

    @DeleteMapping("/{id}")
    void delete(@PathVariable("id") Long id) {
        feedbackService.delete(id);
    }
}
