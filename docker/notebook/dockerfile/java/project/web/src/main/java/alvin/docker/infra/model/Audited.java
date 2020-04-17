package alvin.docker.infra.model;

import java.time.Instant;

public interface Audited extends Identity {
//    Long getCreatedBy();

    Instant getCreatedAt();

//    Long getUpdatedBy();

    Instant getUpdatedAt();
}
