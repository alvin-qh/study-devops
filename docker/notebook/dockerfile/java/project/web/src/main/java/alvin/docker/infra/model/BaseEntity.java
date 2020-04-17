package alvin.docker.infra.model;

import lombok.Getter;
import lombok.ToString;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;

@ToString(onlyExplicitlyIncluded = true)
@MappedSuperclass
public abstract class BaseEntity implements Identity, Cloneable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Access(AccessType.FIELD)
    @ToString.Include
    @Getter
    private Long id;

    @Override
    public Object clone() {
        try {
            return super.clone();
        } catch (CloneNotSupportedException ignore) {
            return null;
        }
    }
}
