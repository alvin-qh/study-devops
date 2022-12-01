package alvin.docker.app.domain.service;

import java.util.List;

import javax.inject.Inject;
import javax.transaction.Transactional;

import org.springframework.stereotype.Component;

import alvin.docker.infra.model.Feedback;
import alvin.docker.infra.repository.FeedbackRepository;

@Component
public class FeedbackService {
    private final FeedbackRepository feedbackRepository;

    @Inject
    public FeedbackService(FeedbackRepository feedbackRepository) {
        this.feedbackRepository = feedbackRepository;
    }

    @Transactional
    public void create(Feedback feedback) {
        feedbackRepository.save(feedback);
    }

    public List<Feedback> list() {
        return feedbackRepository.findAll();
    }

    @Transactional
    public void delete(Long id) {
        feedbackRepository.deleteById(id);
    }
}
