package alvin.docker.infra.model;

import alvin.docker.testing.builder.Builder;
import alvin.docker.testing.builder.EntityBuilder;

@Builder
public class FeedbackBuilder extends EntityBuilder<Feedback> {
    private String title = "Alvin's feedback";
    private String content = "Test feedback";

    @Override
    protected void preBuild(Feedback entity) {
        entity.setTitle(title);
        entity.setContent(content);
    }

    FeedbackBuilder withTitle(String title) {
        this.title = title;
        return this;
    }

    FeedbackBuilder withContent(String content) {
        this.content = content;
        return this;
    }
}
