package alvin.docker.infra.model;

import java.time.Instant;

import javax.persistence.EntityListeners;
import javax.persistence.MappedSuperclass;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@MappedSuperclass
@EntityListeners({ AuditingEntityListener.class })
public abstract class AuditedEntity extends BaseEntity implements Audited {
    @CreatedDate
    protected Instant createdAt;

    @LastModifiedDate
    protected Instant updatedAt;
}
