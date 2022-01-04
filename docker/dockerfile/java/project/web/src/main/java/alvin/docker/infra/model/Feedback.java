package alvin.docker.infra.model;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.Entity;
import javax.persistence.Table;

@Getter
@Setter
@Entity
@Table(name = "docker_feedback")
public class Feedback extends AuditedEntity {
    private String title;
    private String content;

    public Feedback() {
    }

    public Feedback(String title, String content) {
        this.title = title;
        this.content = content;
    }
}
