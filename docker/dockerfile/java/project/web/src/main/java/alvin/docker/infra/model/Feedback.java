package alvin.docker.infra.model;

import javax.persistence.Entity;
import javax.persistence.Table;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "docker_feedback")
public class Feedback extends AuditedEntity {
    private String title;
    private String content;
}
