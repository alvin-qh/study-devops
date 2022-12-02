package alvin.docker.infra.model;

import java.time.Instant;

/**
 * 该接口表示一个具备审计字段的实体对象
 */
public interface Audited extends Identity {
    Instant getCreatedAt();
    Instant getUpdatedAt();
}
