package alvin.docker.infra.model;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;

import lombok.Getter;

@Getter
@MappedSuperclass
public abstract class BaseEntity implements Identity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Access(AccessType.FIELD)
    private Long id;

    @Override
    public boolean equals(Object obj) {
        if (!this.getClass().isInstance(obj)) {
            return false;
        }
        return id.equals(((BaseEntity) obj).getId());
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
