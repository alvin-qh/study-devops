package alvin.docker.domain.service;

import alvin.docker.infra.model.Feedback;
import alvin.docker.infra.repo.FeedbackRepository;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import javax.transaction.Transactional;
import java.util.List;

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
