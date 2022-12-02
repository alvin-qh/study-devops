package alvin.docker.infra.repository;

import java.util.List;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import alvin.docker.infra.model.Feedback;

/**
 * {@link Feedback} 实体类型的持久化类
 */
@Repository
public interface FeedbackRepository extends CrudRepository<Feedback, Long> {
    /**
     * 查询所有持久化对象的集合
     */
    List<Feedback> findAll();

    /**
     * 根据标题字段判断实体对象是否存在
     *
     * @param title 标题字段值
     * @return 对应的实体对象是否存在
     */
    boolean existsByTitle(String title);
}
