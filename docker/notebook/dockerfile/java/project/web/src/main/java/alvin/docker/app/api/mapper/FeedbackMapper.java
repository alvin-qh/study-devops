package alvin.docker.app.api.mapper;

import alvin.docker.app.api.model.FeedbackDto;
import alvin.docker.app.api.model.FeedbackForm;
import alvin.docker.infra.model.Feedback;
import org.springframework.stereotype.Component;

@Component
public class FeedbackMapper {

    public Feedback toEntity(FeedbackForm form) {
        return new Feedback(form.getTitle(), form.getContent());
    }

    public FeedbackDto toDto(Feedback entity) {
        return new FeedbackDto(entity.getId(), entity.getTitle(), entity.getContent(),
                entity.getCreatedAt(), entity.getUpdatedAt());
    }
}
