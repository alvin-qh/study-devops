package alvin.docker.builder;

import java.util.concurrent.atomic.AtomicInteger;

import alvin.docker.infra.model.Feedback;

public class FeedbackBuilder extends Builder<Feedback> {
    private static final AtomicInteger SEQUENCE = new AtomicInteger(1);

    private String title = "Feedback_" + SEQUENCE.getAndIncrement();
    private String content = "Test feedback";

    public FeedbackBuilder withTitle(String title) {
        this.title = title;
        return this;
    }

    public FeedbackBuilder withContent(String content) {
        this.content = content;
        return this;
    }

    @Override
    public Feedback build() {
        var entity = new Feedback();
        entity.setTitle(title);
        entity.setContent(content);
        return entity;
    }
}
