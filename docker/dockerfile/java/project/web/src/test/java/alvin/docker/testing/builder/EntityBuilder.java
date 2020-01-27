package alvin.docker.testing.builder;

import alvin.docker.infra.model.BaseEntity;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

public abstract class EntityBuilder<T extends BaseEntity> {
    @Inject
    private EntityManager em;

    private T entity;

    @SuppressWarnings("unchecked")
    public EntityBuilder() {
        Class<?> type = getClass();
        Type t = type.getGenericSuperclass();
        if (t instanceof ParameterizedType) {
            type = (Class<?>) ((ParameterizedType) t).getActualTypeArguments()[0];
            try {
                entity = (T) type.newInstance();
            } catch (InstantiationException | IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        } else {
            throw new RuntimeException("Cannot create entity object");
        }
    }

    protected void preBuild(T entity) {
    }

    protected void preCreate(T entity) {
    }

    EntityManager em() {
        return em;
    }

    public T build() {
        final T entity = this.entity;
        this.entity = (T) entity.clone();
        preBuild(entity);
        return entity;
    }

    public T create() {
        T entity = build();
        preCreate(entity);
        em.persist(entity);
        return entity;
    }
}
