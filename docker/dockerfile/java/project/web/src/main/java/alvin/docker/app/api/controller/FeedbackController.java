package alvin.docker.app.api.controller;

import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.validation.Valid;

import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import alvin.docker.app.api.mapper.FeedbackMapper;
import alvin.docker.app.api.model.FeedbackDto;
import alvin.docker.app.api.model.FeedbackForm;
import alvin.docker.app.api.model.Response;
import alvin.docker.app.common.error.ClientError;
import alvin.docker.app.common.error.HttpClientException;
import alvin.docker.app.domain.service.FeedbackService;

@RestController
@RequestMapping("/api/feedback")
public class FeedbackController {
    private final FeedbackService feedbackService;
    private final FeedbackMapper feedbackMapper;

    @Inject
    public FeedbackController(FeedbackService feedbackService,
                              FeedbackMapper feedbackMapper) {
        this.feedbackService = feedbackService;
        this.feedbackMapper = feedbackMapper;
    }

    @GetMapping
    Response<List<FeedbackDto>> index() {
        var feedback = feedbackService.list()
                .stream()
                .map(feedbackMapper::toDto)
                .collect(Collectors.toList());
        return Response.success(feedback);
    }

    @PostMapping
    Response<Void> create(@RequestBody @Valid FeedbackForm form, BindingResult br) {
        if (br.hasErrors()) {
            throw new HttpClientException(ClientError.badRequest().build());
        }
        var feedback = feedbackMapper.toEntity(form);
        feedbackService.create(feedback);
        return Response.success(null);
    }

    @DeleteMapping("/{id}")
    Response<Void> delete(@PathVariable("id") Long id) {
        feedbackService.delete(id);
        return Response.success(null);
    }
}
