package alvin.docker.infra.model;

import java.time.Instant;

public interface Audited extends Identity {
    Instant getCreatedAt();
    Instant getUpdatedAt();
}
