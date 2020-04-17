package alvin.docker.infra.model;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.EntityListeners;
import javax.persistence.MappedSuperclass;
import java.time.Instant;

@MappedSuperclass
@EntityListeners({AuditingEntityListener.class})
public abstract class AuditedEntity extends BaseEntity implements Audited {
//    @Getter
//    @Setter
//    @CreatedBy
//    protected Long createdBy;

    @Getter
    @Setter
    @CreatedDate
    protected Instant createdAt;

//    @Getter
//    @Setter
//    @LastModifiedBy
//    protected Long updatedBy;

    @Getter
    @Setter
    @LastModifiedDate
    protected Instant updatedAt;
}
