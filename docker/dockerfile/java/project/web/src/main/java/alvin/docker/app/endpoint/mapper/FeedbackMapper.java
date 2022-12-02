package alvin.docker.app.endpoint.mapper;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import alvin.docker.app.endpoint.model.FeedbackDto;
import alvin.docker.app.endpoint.model.FeedbackForm;
import alvin.docker.infra.model.Feedback;

@Scope(BeanDefinition.SCOPE_PROTOTYPE)
@Component
public class FeedbackMapper {

    public Feedback toEntity(FeedbackForm form) {
        var entity = new Feedback();
        entity.setTitle(form.getTitle());
        entity.setContent(form.getContent());
        return entity;
    }

    public FeedbackDto toDto(Feedback entity) {
        return new FeedbackDto(
                entity.getId(),
                entity.getTitle(),
                entity.getContent(),
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }
}
