package alvin.docker.infra.model;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;

import lombok.Getter;

/**
 * 实现 {@code Identity} 接口, 添加 {@code id} 字段, 实现 {@code #getId()} 方法
 */
@Getter
@MappedSuperclass
public abstract class BaseEntity implements Identity {
    // id 字段, 主键
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
