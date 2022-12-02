package alvin.docker.app.endpoint.mapper;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import alvin.docker.app.endpoint.model.FeedbackDto;
import alvin.docker.app.endpoint.model.FeedbackForm;
import alvin.docker.infra.model.Feedback;

/**
 * 对象类型转换器
 */
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
@Component
public class FeedbackMapper {
    /**
     * 将 Form 类型转为 Entity 类型对象
     *
     * @param form {@link FeedbackForm} 对象
     * @return {@link Feedback} 实体对象
     */
    public Feedback toEntity(FeedbackForm form) {
        var entity = new Feedback();
        entity.setTitle(form.getTitle());
        entity.setContent(form.getContent());
        return entity;
    }

    /**
     * 将 Entity 类型转为 DTO 类型对象
     *
     * @param form {@link Feedback} 实体对象
     * @return {@link FeedbackDto} DTO 对象
     */
    public FeedbackDto toDto(Feedback entity) {
        return new FeedbackDto(
                entity.getId(),
                entity.getTitle(),
                entity.getContent(),
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }
}
