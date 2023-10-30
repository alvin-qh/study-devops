package alvin.docker.builder;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

/**
 * 实体构建器超类
 */
public abstract class Builder<T> {
    // 实体管理器对象
    @PersistenceContext
    protected EntityManager em;

    /**
     * 创建实体对象 (非持久化)
     */
    public abstract T build();

    /**
     * 创建实体对象 (持久化)
     */
    public T create() {
        var obj = build();
        em.persist(obj);
        return obj;
    }
}
