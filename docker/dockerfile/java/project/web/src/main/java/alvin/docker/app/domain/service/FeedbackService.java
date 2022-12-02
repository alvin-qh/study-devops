package alvin.docker.app.domain.service;

import java.util.List;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import alvin.docker.infra.model.Feedback;
import alvin.docker.infra.repository.FeedbackRepository;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class FeedbackService {
    private final FeedbackRepository feedbackRepository;

    @Transactional
    public void create(Feedback feedback) {
        feedbackRepository.save(feedback);
    }

    @Transactional(readOnly = true)
    public List<Feedback> list() {
        return feedbackRepository.findAll();
    }

    @Transactional
    public void delete(Long id) {
        feedbackRepository.deleteById(id);
    }
}
