package alvin.docker.app.api.model;

import lombok.Getter;

import java.time.Instant;

@Getter
public class FeedbackDto {
    private Long id;
    private String title;
    private String content;
    private Instant createdAt;
    private Instant updatedAt;

    public FeedbackDto(Long id, String title, String content, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }
}
