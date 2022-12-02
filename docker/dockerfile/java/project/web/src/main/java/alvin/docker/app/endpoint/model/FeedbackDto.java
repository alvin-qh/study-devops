package alvin.docker.app.endpoint.model;

import java.io.Serializable;
import java.time.Instant;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor(access = AccessLevel.PACKAGE)
@AllArgsConstructor
public class FeedbackDto implements Serializable {
    private Long id;
    private String title;
    private String content;
    private Instant createdAt;
    private Instant updatedAt;
}
