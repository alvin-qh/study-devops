package alvin.docker.infra.model;

import java.time.Instant;

import javax.persistence.EntityListeners;
import javax.persistence.MappedSuperclass;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 实现 {@link Audited} 接口, 添加审计字段并实现 {@link #getCreatedAt()} 和
 * {@link #getUpdatedAt()} 方法
 *
 * <p>
 * {@link AuditingEntityListener} 监听器会在执行插入和更新操作时, 自动填充字段值
 * </p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@EntityListeners({ AuditingEntityListener.class })
@MappedSuperclass
public abstract class AuditedEntity extends BaseEntity implements Audited {
    @CreatedDate
    protected Instant createdAt;

    @LastModifiedDate
    protected Instant updatedAt;
}
