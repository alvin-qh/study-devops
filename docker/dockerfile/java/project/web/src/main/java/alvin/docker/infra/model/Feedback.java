package alvin.docker.infra.model;

import javax.persistence.Entity;
import javax.persistence.Table;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 表示一条反馈意见的实体类型
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "docker_feedback")
public class Feedback extends AuditedEntity {
    // 标题
    private String title;

    // 内容
    private String content;
}
